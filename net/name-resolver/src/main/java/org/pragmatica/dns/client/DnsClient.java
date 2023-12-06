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
import org.pragmatica.dns.ResolverErrors.InvalidResponse;
import org.pragmatica.dns.ResolverErrors.RequestTimeout;
import org.pragmatica.dns.ResolverErrors.ServerError;
import org.pragmatica.dns.inet.InetUtils;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.AsyncCloseable;
import org.pragmatica.lang.io.Timeout;
import org.pragmatica.net.transport.api.TransportConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.option;
import static org.pragmatica.lang.Unit.unitResult;
import static org.pragmatica.lang.io.Timeout.timeout;

public interface DnsClient extends AsyncCloseable {
    Logger log = LoggerFactory.getLogger(DnsClient.class);

    Promise<DomainAddress> resolve(DomainName domainName, InetSocketAddress serverAddress);

    static DnsClient create() {
        var bootstrap = TransportConfiguration
            .transportConfiguration()
            .datagramBootstrap();

        var client = DnsClientImpl.forBootstrap(bootstrap);

        bootstrap.handler(DnsChannelInitializer.forClient(client));

        return client;
    }
}

class DnsChannelInitializer extends ChannelInitializer<DatagramChannel> {
    private final DnsClientImpl client;

    private DnsChannelInitializer(DnsClientImpl client) {
        this.client = client;
    }

    static DnsChannelInitializer forClient(DnsClientImpl client) {
        return new DnsChannelInitializer(client);
    }

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
}

record Request(DomainName domainName, Promise<DomainAddress> promise, int requestId) {}

record DnsClientImpl(Bootstrap bootstrap, ConcurrentHashMap<Integer, Request> requestMap, AtomicInteger idCounter) implements DnsClient {
    private static final Timeout QUERY_TIMEOUT = timeout(10).seconds();

    static DnsClientImpl forBootstrap(Bootstrap bootstrap) {
        return new DnsClientImpl(bootstrap, new ConcurrentHashMap<>(), new AtomicInteger(1));
    }

    @Override
    public Promise<DomainAddress> resolve(DomainName domainName, InetSocketAddress serverAddress) {
        return Promise.promise(promise -> fireRequest(promise, domainName, serverAddress));
    }

    @SuppressWarnings("resource")
    @Override
    public Promise<Unit> close() {
        return Promise.promise(promise -> bootstrap().config().group()
                                                     .shutdownGracefully()
                                                     .addListener(_ -> promise.resolve(unitResult())));
    }

    void handleDatagram(DatagramDnsResponse msg) {
        var requestId = msg.id();

        log.debug("Received response for request Id {}", requestId);

        option(requestMap.get(requestId))
            .onPresent(request -> handleResponse(request, msg));
    }

    private void handleResponse(Request request, DatagramDnsResponse msg) {
        log.debug("Handling response {} for request {}", msg, request);

        if (!msg.code().equals(DnsResponseCode.NOERROR)) {
            var errorMessage = STR. "Server responded with error code \{ msg.code() }" ;

            log.warn(errorMessage);

            request.promise()
                   .failure(new ServerError(errorMessage));
            return;
        }

        var addresses = new ArrayList<DomainAddress>();
        for (int i = 0, count = msg.count(DnsSection.ANSWER); i < count; i++) {
            var record = msg.recordAt(DnsSection.ANSWER, i);

            if (record.type() == DnsRecordType.A) {
                var raw = (DnsRawRecord) record;

                log.info("record {}, ttl {}", raw, raw.timeToLive());

                InetUtils.forBytes(ByteBufUtil.getBytes(raw.content()))
                         .map(inetAddress -> DomainAddress.domainAddress(request.domainName(), inetAddress,
                                                                         Duration.ofSeconds(raw.timeToLive())))
                         .onSuccess(addresses::add)
                         .onFailureDo(() -> log.warn("Response for {} contains incorrectly formatted IP address", request.domainName()));
            }
        }

        addresses.stream()
                 .min(Comparator.comparing(DomainAddress::ttl))
                 .ifPresentOrElse(address -> request.promise().success(address),
                                  () -> request.promise().failure(new ServerError("No address provided by server")));
    }

    private void fireRequest(Promise<DomainAddress> promise, DomainName domainName, InetSocketAddress serverAddress) {
        bootstrap().bind(0)
                   .syncUninterruptibly()
                   .channel()
                   .writeAndFlush(buildQuery(serverAddress, promise, domainName));

        // Setup guard timeout
        promise.async(QUERY_TIMEOUT,
                      pending -> pending.failure(new RequestTimeout("No response from server in 10 seconds")));
    }

    private DatagramDnsQuery buildQuery(InetSocketAddress serverAddress, Promise<DomainAddress> promise, DomainName domainName) {
        var request = computeRequest(promise, domainName);

        log.debug("Sending request {} to {}", request, serverAddress);

        return new DatagramDnsQuery(null, serverAddress, request.requestId())
            .setRecursionDesired(true)
            .setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(request.domainName().name(), DnsRecordType.A));
    }

    private Request computeRequest(Promise<DomainAddress> promise, DomainName domainName) {
        for (int attempt = 0; attempt < 0xFFFF; attempt++) {
            var requestId = idCounter().getAndIncrement() & 0xFFFF;
            var request = new Request(domainName, promise, requestId);

            if (requestMap().putIfAbsent(requestId, request) == null) {
                // Ensure slot for this ID is freed regardless of the outcome
                promise.onResultDo(() -> requestMap().remove(requestId));
                return request;
            }
        }
        throw new IllegalStateException("Unable to generate request id (too many requests in progress)");
    }
}
