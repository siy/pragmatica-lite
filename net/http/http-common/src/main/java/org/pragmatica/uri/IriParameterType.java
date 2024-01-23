package org.pragmatica.uri;

import com.google.auto.service.AutoService;
import org.pragmatica.config.api.DataConversionError;
import org.pragmatica.config.api.ParameterType.CustomParameterType;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

@AutoService(CustomParameterType.class)
@SuppressWarnings("rawtypes") // Required by AutoService because CustomParameterType is generic
public class IriParameterType implements CustomParameterType<IRI> {
    @Override
    public TypeToken<IRI> token() {
        return new TypeToken<>() {};
    }

    @Override
    public Result<IRI> apply(String value) {
        var iri = IRI.fromString(value);

        return iri == IRI.EMPTY ? new DataConversionError.InvalidInput(STR."The value [\{value}] can't be parsed into IRI").result()
                                : Result.success(iri);
    }
}
