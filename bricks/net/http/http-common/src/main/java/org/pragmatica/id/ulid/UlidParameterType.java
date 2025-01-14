package org.pragmatica.id.ulid;

import com.google.auto.service.AutoService;
import org.pragmatica.config.api.ParameterType.CustomParameterType;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

@AutoService(CustomParameterType.class)
public class UlidParameterType implements CustomParameterType<ULID> {
    @Override
    public TypeToken<ULID> token() {
        return new TypeToken<>() {};
    }

    @Override
    public Result<ULID> apply(String input) {
        return ULID.parse(input);
    }
}
