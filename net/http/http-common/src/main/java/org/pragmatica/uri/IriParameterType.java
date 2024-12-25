package org.pragmatica.uri;

import com.google.auto.service.AutoService;
import org.pragmatica.config.api.DataConversionError;
import org.pragmatica.config.api.ParameterType.CustomParameterType;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import static org.pragmatica.lang.Option.none;

@AutoService(CustomParameterType.class)
@SuppressWarnings("unused")
public class IriParameterType implements CustomParameterType<IRI> {
    @Override
    public TypeToken<IRI> token() {
        return new TypeToken<>() {};
    }

    @Override
    public Result<IRI> apply(String value) {
        var iri = IRI.fromString(value);

        return iri == IRI.EMPTY ? new DataConversionError.InvalidInput("The value [" + value + "] can't be parsed into IRI", none()).result()
                                : Result.success(iri);
    }
}
