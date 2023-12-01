package org.pragmatica.codec.json.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import org.pragmatica.codec.json.JsonCodecFactory;

@AutoService(JsonCodecFactory.class)
public final class JacksonJsonCodecFactory implements JsonCodecFactory<JacksonJsonCodecConfiguration> {
    @Override
    public JacksonJsonCodec forConfiguration(JacksonJsonCodecConfiguration config) {
        return JacksonJsonCodec.forMapper(config.configureMapper(new ObjectMapper()));
    }

    @Override
    public JacksonJsonCodec withDefaultConfiguration() {
        return JacksonJsonCodec.forMapper(new ObjectMapper());
    }

    @Override
    public Class<JacksonJsonCodecConfiguration> configClass() {
        return JacksonJsonCodecConfiguration.class;
    }
}
