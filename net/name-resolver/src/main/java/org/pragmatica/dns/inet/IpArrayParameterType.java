package org.pragmatica.dns.inet;

import com.google.auto.service.AutoService;
import org.pragmatica.config.api.ParameterType.CustomParameterType;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.net.InetAddress;
import java.util.List;

@AutoService(CustomParameterType.class)
@SuppressWarnings("rawtypes") // Required by AutoService because CustomParameterType is generic
public class IpArrayParameterType implements CustomParameterType<List<InetAddress>> {
    @Override
    public TypeToken<List<InetAddress>> token() {
        return new TypeToken<>() {};
    }

    @Override
    public Result<List<InetAddress>> apply(String param1) {
        return StringArrayParameter.tryParse(param1, "IP address array")
                                   .map(stream -> stream.map(IpParameterType.INSTANCE::apply))
                                   .flatMap(Result::allOf);
    }
}