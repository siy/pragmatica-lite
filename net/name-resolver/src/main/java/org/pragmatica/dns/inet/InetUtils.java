package org.pragmatica.dns.inet;

import org.pragmatica.dns.ResolverErrors.InvalidIpAddress;
import org.pragmatica.dns.ResolverErrors.UnknownError;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Result.Cause;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class InetUtils {
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
