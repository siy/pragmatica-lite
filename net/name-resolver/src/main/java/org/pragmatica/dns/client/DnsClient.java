package org.pragmatica.dns.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.codec.dns.*;
import org.pragmatica.dns.DomainAddress;
import org.pragmatica.dns.DomainName;
import org.pragmatica.dns.ResolverErrors;
import org.pragmatica.dns.ResolverErrors.RequestTimeout;
import org.pragmatica.dns.inet.InetUtils;
import org.pragmatica.lang.Promise;
import org.pragmatica.net.transport.api.TransportConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.pragmatica.dns.DomainName.domainName;
import static org.pragmatica.lang.Option.option;
import static org.pragmatica.lang.io.Timeout.timeout;

public interface DnsClient {
    static Logger log = LoggerFactory.getLogger(DnsClient.class);
    default Promise<DomainAddress> resolve(String domainName, InetAddress serverAddress, int serverPort) {
        return resolve(domainName(domainName), new InetSocketAddress(serverAddress, serverPort));
    }

    Promise<DomainAddress> resolve(DomainName domainName, InetSocketAddress serverAddress);

    static DnsClient create() {
        record request(DomainName domainName, Promise<DomainAddress> promise, int requestId) {
        }
        record dnsClient(Bootstrap bootstrap, ConcurrentHashMap<Integer, request> requestMap, AtomicInteger idCounter) implements DnsClient {
            @Override
            public Promise<DomainAddress> resolve(DomainName domainName, InetSocketAddress serverAddress) {
                return Promise.promise(promise -> fireRequest(promise, domainName, serverAddress));
            }

            private void handleDatagram(DatagramDnsResponse msg) {
                var requestId = msg.id();
                log.info("Received response for request {}", requestId);

                option(requestMap.remove(requestId))
                    .onPresent(request -> handleResponse(request, msg));
            }

            private void handleResponse(request request, DatagramDnsResponse msg) {
                log.info("Handling response for request {}", request);
                for (int i = 0, count = msg.count(DnsSection.ANSWER); i < count; i++) {
                    var record = msg.recordAt(DnsSection.ANSWER, i);

                    if (record.type() == DnsRecordType.A) {
                        var raw = (DnsRawRecord) record;

                        var address = InetUtils.forBytes(ByteBufUtil.getBytes(raw.content()))
                                               .map(inetAddress -> DomainAddress.domainAddress(request.domainName(), inetAddress,
                                                                                               Duration.ofSeconds(raw.timeToLive())))
                                               .mapError(_ -> new ResolverErrors.InvalidResponse("IP address provided by server is invalid"));

                        request.promise().resolve(address);
                    }
                }
            }

            private void fireRequest(Promise<DomainAddress> promise, DomainName domainName, InetSocketAddress serverAddress) {
                var request = computeRequest(promise, domainName);

                log.info("Sending request {} to {}", request, serverAddress);

                var query = new DatagramDnsQuery(null, serverAddress, request.requestId())
                    .setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(domainName.name(), DnsRecordType.A));

                bootstrap()
                    .bind(serverAddress)
                    .syncUninterruptibly()
                    .channel()
                    .writeAndFlush(query);

                Promise.runAsync(timeout(10).seconds(),
                                 () -> handleTimeout(request));
            }

            private void handleTimeout(request request) {
                option(requestMap().remove(request.requestId()))
                    .onPresent(pending -> pending.promise()
                                                 .failure(new RequestTimeout("No response from server in 10 seconds")));
            }

            private request computeRequest(Promise<DomainAddress> promise, DomainName domainName) {
                for (int attempt = 0; attempt < 0xFFFF; attempt++) {
                    var requestId = idCounter().getAndIncrement() & 0xFFFF;

                    log.info("Generated request id {}", requestId);
                    var value = new request(domainName, promise, requestId);

                    if (requestMap().putIfAbsent(requestId, value) == null) {
                        return value;
                    }
                }
                throw new IllegalStateException("Unable to generate request id (too many requests in progress)");
            }
        }

        var bootstrap = TransportConfiguration
            .transportConfiguration()
            .datagramBootstrap();

        var client = new dnsClient(bootstrap,
                                   new ConcurrentHashMap<>(),
                                   new AtomicInteger(1));

        bootstrap
            .handler(new ChannelInitializer<DatagramChannel>() {
                @Override
                protected void initChannel(DatagramChannel ch) {
                    ch.pipeline()
                      .addLast(new DatagramDnsQueryEncoder())
                      .addLast(new DatagramDnsResponseDecoder())
                      .addLast(new SimpleChannelInboundHandler<DatagramDnsResponse>() {
                          @Override
                          protected void channelRead0(ChannelHandlerContext ctx, DatagramDnsResponse msg) {
                              try {
                                  client.handleDatagram(msg);
                              } finally {
                                  ctx.close();
                              }
                          }
                      });
                }
            });

        return client;
    }
}
