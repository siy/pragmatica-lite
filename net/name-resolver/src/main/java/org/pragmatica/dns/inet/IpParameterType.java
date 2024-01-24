package org.pragmatica.dns.inet;

import com.google.auto.service.AutoService;
import org.pragmatica.config.api.ParameterType.CustomParameterType;
import org.pragmatica.dns.ResolverErrors;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.lang.utils.Causes;

import java.net.InetAddress;

@AutoService(CustomParameterType.class)
@SuppressWarnings("rawtypes") // Required by AutoService because CustomParameterType is generic
public class IpParameterType implements CustomParameterType<InetAddress> {
    private static final ShortParameter PARSER = ShortParameter.INSTANCE;

    public static final IpParameterType INSTANCE = new IpParameterType();

    @Override
    public TypeToken<InetAddress> token() {
        return new TypeToken<>() {};
    }

    @Override
    public Result<InetAddress> apply(String value) {
        if (value.isEmpty()) {
            return new ResolverErrors.InvalidIpAddress("Ïnput address is empty").result();
        }
        var split = value.split("\\.");

        if (split.length != 4) {
            return new ResolverErrors.InvalidIpAddress("Ïnput address {%s} is invalid".formatted(value)).result();
        }

        return Result.all(PARSER.apply(split[0]).flatMap(IpParameterType::validateFirst),
                          PARSER.apply(split[1]).flatMap(IpParameterType::validate),
                          PARSER.apply(split[2]).flatMap(IpParameterType::validate),
                          PARSER.apply(split[3]).flatMap(IpParameterType::validate))
                     .flatMap((d1, d2, d3, d4) -> toAddress(d1.byteValue(), d2.byteValue(), d3.byteValue(), d4.byteValue()));
    }

    private Result<InetAddress> toAddress(byte... digits) {
        return InetUtils.forBytes(digits);
    }

    private static Result<Short> validate(Short aShort) {
        return aShort >= 0 && aShort <= 255
               ? Result.success(aShort)
               : Causes.cause("Invalid number")
                       .result();
    }

    private static Result<Short> validateFirst(Short aShort) {
        return aShort > 0 && aShort <= 255
               ? Result.success(aShort)
               : Causes.cause("Invalid number")
                       .result();
    }
}