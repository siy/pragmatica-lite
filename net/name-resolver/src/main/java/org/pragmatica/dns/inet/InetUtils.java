package org.pragmatica.dns.inet;

import org.pragmatica.dns.ResolverErrors;
import org.pragmatica.lang.Result;

import java.net.InetAddress;
import java.net.UnknownHostException;

public sealed interface InetUtils {
    static Result<InetAddress> forBytes(byte[] address) {
        return Result.lift(InetUtils::exceptionMapper, () -> InetAddress.getByAddress(address));
    }

    static Result.Cause exceptionMapper(Throwable throwable) {
        return switch (throwable) {
            case UnknownHostException unknownHostException -> new ResolverErrors.InvalidIpAddress(unknownHostException.getMessage());
            default -> new ResolverErrors.UnknownError(throwable.getMessage());
        };
    }

    record unused() implements InetUtils {}
}
