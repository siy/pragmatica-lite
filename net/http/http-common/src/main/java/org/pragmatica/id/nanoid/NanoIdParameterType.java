package org.pragmatica.id.nanoid;


import com.google.auto.service.AutoService;
import org.pragmatica.config.api.ParameterType.CustomParameterType;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

@AutoService(CustomParameterType.class)
public class NanoIdParameterType implements CustomParameterType<NanoId> {
    @Override
    public TypeToken<NanoId> token() {
        return new TypeToken<>() {};
    }

    @Override
    public Result<NanoId> apply(String input) {
        return NanoId.parse(input);
    }
}
