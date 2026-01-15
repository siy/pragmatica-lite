/*
 *  Copyright (c) 2022-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.net.dns;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.AsyncCloseable;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.net.dns.ResolverErrors.UnknownDomain;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.lang.Option.option;
import static org.pragmatica.lang.Promise.resolved;
import static org.pragmatica.lang.Promise.success;
import static org.pragmatica.net.dns.DomainAddress.domainAddress;
import static org.pragmatica.net.dns.DomainName.domainName;

/// Asynchronous domain name resolver with TTL-based caching.
///
/// The cache uses promise mechanics for automatic TTL expiry - no background threads required.
/// Each successful resolution schedules its own eviction via `promise.async(ttl, ...)`.
public interface DomainNameResolver extends AsyncCloseable {
    int DNS_UDP_PORT = 53;
    Result<DomainAddress> UNKNOWN_DOMAIN = new UnknownDomain("Unknown domain").result();

    /// Resolve domain name, using cache if available.
    default Promise<DomainAddress> resolve(String name) {
        return resolve(domainName(name));
    }

    /// Resolve domain name, using cache if available.
    Promise<DomainAddress> resolve(DomainName name);

    /// Get cached resolution result without triggering new resolution.
    default Promise<DomainAddress> resolveCached(String name) {
        return resolveCached(domainName(name));
    }

    /// Get cached resolution result without triggering new resolution.
    Promise<DomainAddress> resolveCached(DomainName name);

    /// Create resolver with provided DNS servers, using own event loop.
    /// The event loop will be shut down when the resolver is closed.
    ///
    /// @param servers list of DNS server addresses
    /// @return new resolver instance
    static DomainNameResolver domainNameResolver(List<InetAddress> servers) {
        var eventLoop = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        return new Resolver(DnsClient.dnsClient(eventLoop), prepareServers(servers), buildDnsCache(), eventLoop, true);
    }

    /// Create resolver with provided DNS servers, sharing the given event loop.
    /// The event loop will NOT be shut down when the resolver is closed.
    ///
    /// @param servers    list of DNS server addresses
    /// @param eventLoop  event loop group to use for DNS queries
    /// @return new resolver instance
    static DomainNameResolver domainNameResolver(List<InetAddress> servers, EventLoopGroup eventLoop) {
        return new Resolver(DnsClient.dnsClient(eventLoop), prepareServers(servers), buildDnsCache(), eventLoop, false);
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
                                              Duration.ofSeconds(0));
        cache.put(resolvedLocalHost.name(), success(resolvedLocalHost));
        return cache;
    }
}

record Resolver(DnsClient client,
                List<InetSocketAddress> serverList,
                ConcurrentHashMap<DomainName, Promise<DomainAddress>> cache,
                EventLoopGroup eventLoop,
                boolean ownsEventLoop) implements DomainNameResolver {
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
                                     log.debug("TTL for {} is {}",
                                               domainAddress.name(),
                                               domainAddress.ttl());
                                     var ttl = TimeSpan.fromDuration(domainAddress.ttl());
                                     promise.async(ttl,
                                                   _ -> {
                                                       cache.remove(domainAddress.name());
                                                       log.debug("TTL expired, removed {} from cache",
                                                                 domainAddress.name());
                                                   });
                                 });
    }

    private Promise<DomainAddress> fireRequest(DomainName domainName) {
        return Promise.any(UNKNOWN_DOMAIN,
                           serverList.stream()
                                     .map(server -> client.resolve(domainName, server))
                                     .toList());
    }

    @Override
    public Promise<Unit> close() {
        if (!ownsEventLoop) {
            return client.close();
        }
        return client.close()
                     .flatMap(_ -> Promise.promise(promise -> eventLoop.shutdownGracefully()
                                                                       .addListener(_ -> promise.succeed(Unit.unit()))));
    }
}
