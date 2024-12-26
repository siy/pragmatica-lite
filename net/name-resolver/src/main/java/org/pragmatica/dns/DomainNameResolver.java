package org.pragmatica.dns;

import org.pragmatica.dns.ResolverErrors.UnknownDomain;
import org.pragmatica.dns.client.DnsClient;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.AsyncCloseable;
import org.pragmatica.lang.io.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.pragmatica.config.api.AppConfig.appConfig;
import static org.pragmatica.dns.DomainAddress.domainAddress;
import static org.pragmatica.dns.DomainName.domainName;
import static org.pragmatica.lang.Option.option;
import static org.pragmatica.lang.Promise.resolved;
import static org.pragmatica.lang.Promise.success;

public interface DomainNameResolver extends AsyncCloseable {
    int DNS_UDP_PORT = 53;
    Result<DomainAddress> UNKNOWN_DOMAIN = new UnknownDomain("Unknown domain").result();

    default Promise<DomainAddress> resolve(String name) {
        return resolve(domainName(name));
    }

    Promise<DomainAddress> resolve(DomainName name);

    default Promise<DomainAddress> resolveCached(String name) {
        return resolveCached(domainName(name));
    }

    Promise<DomainAddress> resolveCached(DomainName name);

    static DomainNameResolver defaultResolver() {
        return DefaultResolverHolder.INSTANCE.resolver();
    }

    static DomainNameResolver with(List<InetAddress> serverList) {
        record Resolver(DnsClient client, List<InetSocketAddress> serverList, ConcurrentHashMap<DomainName, Promise<DomainAddress>> cache)
            implements DomainNameResolver {
            private static final Logger log = LoggerFactory.getLogger(DomainNameResolver.class);

            @Override
            public Promise<DomainAddress> resolveCached(DomainName name) {
                return option(cache.get(name)).or(() -> resolved(UNKNOWN_DOMAIN));
            }

            @Override
            public Promise<DomainAddress> resolve(DomainName name) {
                var promise = cache.computeIfAbsent(name, this::fireRequest);

                // Do not cache failed requests and observe TTL for successful ones
                return promise.onFailureRun(() -> cache.remove(name))
                              .onSuccess(domainAddress -> {
                                  log.debug("TTL for {} is {}", domainAddress.name(), domainAddress.ttl());
                                  promise.async(Timeout.fromDuration(domainAddress.ttl()),
                                                _ -> log.debug("TTL expired, removing {} from cache", cache.remove(domainAddress.name())));
                              });
            }

            private Promise<DomainAddress> fireRequest(DomainName domainName) {
                return Promise.anySuccess(UNKNOWN_DOMAIN,
                                          serverList.stream()
                                                    .map(server -> client.resolve(domainName, server))
                                                    .toList());
            }

            @Override
            public Promise<Unit> close() {
                return client().close();
            }
        }

        return new Resolver(DnsClient.create(), prepareServers(serverList), buildDnsCache());
    }

    private static List<InetSocketAddress> prepareServers(List<InetAddress> serverList) {
        return serverList.stream()
                         .map(ip -> new InetSocketAddress(ip, DNS_UDP_PORT))
                         .toList();
    }

    private static ConcurrentHashMap<DomainName, Promise<DomainAddress>> buildDnsCache() {
        var cache = new ConcurrentHashMap<DomainName, Promise<DomainAddress>>();
        var resolvedLocalHost = domainAddress(domainName("localhost"),
                                              InetAddress.getLoopbackAddress(),
                                              Duration.ofSeconds(0));   //Duration does not matter for localhost

        cache.put(resolvedLocalHost.name(), success(resolvedLocalHost));
        return cache;
    }
}

enum DefaultResolverHolder {
    INSTANCE;

    private final DomainNameResolver resolver;

    {
        var log = LoggerFactory.getLogger(DefaultResolverHolder.class);

        var serverList = appConfig("dns", DnsServers.template())
            .map(DnsServers::servers)
            .fold(cause -> {
                log.error("Failed to load DNS servers from config, using default ones: {}", cause);
                throw new IllegalStateException("Unable to start DNS resolver: %s".formatted(cause));
            }, servers -> {
                log.debug("Loaded DNS servers from config: {}", servers);
                return servers;
            });

        resolver = DomainNameResolver.with(serverList);

        Runtime.getRuntime().addShutdownHook(new Thread(resolver::close));
    }

    public DomainNameResolver resolver() {
        return resolver;
    }
}