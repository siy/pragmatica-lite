package org.pragmatica.db.postgres;

import com.google.auto.service.AutoService;
import org.pragmatica.config.api.ParameterType.CustomParameterType;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.uri.IRI;

@AutoService(CustomParameterType.class)
public class IriParameterType implements CustomParameterType<IRI> {
    @Override
    public TypeToken<IRI> token() {
        return new TypeToken<IRI>() {};
    }

    @Override
    public Result<IRI> apply(String param1) {
        return null;
    }
}
