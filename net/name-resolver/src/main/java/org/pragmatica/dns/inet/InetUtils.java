package org.pragmatica.dns.inet;

import org.pragmatica.dns.ResolverErrors.InvalidIpAddress;
import org.pragmatica.dns.ResolverErrors.UnknownError;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Result.Cause;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public final class InetUtils {
    public static final List<InetAddress> DEFAULT_DNS_SERVERS;

    static {
        try {
            DEFAULT_DNS_SERVERS = List.of(
                InetAddress.getByAddress(new byte[]{1, 0, 0, 1}),                               // Cloudflare
                InetAddress.getByAddress(new byte[]{1, 1, 1, 1}),                               // Cloudflare
                InetAddress.getByAddress(new byte[]{8, 8, 4, 4}),                               // Google Public DNS
                InetAddress.getByAddress(new byte[]{8, 8, 8, 8}),                               // Google Public DNS
                InetAddress.getByAddress(new byte[]{9, 9, 9, 9}),                               // Quad9
                InetAddress.getByAddress(new byte[]{(byte) 149, 112, 112, 112}),                // Quad9
                InetAddress.getByAddress(new byte[]{(byte) 208, 67, (byte) 220, (byte) 220}),   // Cisco OpenDNS
                InetAddress.getByAddress(new byte[]{(byte) 208, 67, (byte) 222, (byte) 222}));    // Cisco OpenDNS
        } catch (UnknownHostException e) {
            // Should never happen, all addresses are valid IPv4
            throw new RuntimeException(e);
        }
    }

    public static Result<InetAddress> forBytes(byte[] address) {
        return Result.lift(InetUtils::exceptionMapper, () -> InetAddress.getByAddress(address));
    }

    static Cause exceptionMapper(Throwable throwable) {
        if (throwable instanceof UnknownHostException) {
            return new InvalidIpAddress(throwable.getMessage());
        }
        return new UnknownError(throwable.getMessage());
    }

    private InetUtils() {}
}
