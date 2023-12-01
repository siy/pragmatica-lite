package org.pragmatica.codec.json.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pragmatica.codec.json.JsonCodecConfiguration;

//TODO: add configuration options
public class JacksonJsonCodecConfiguration implements JsonCodecConfiguration {
    public ObjectMapper configureMapper(ObjectMapper objectMapper) {
        return objectMapper;
    }
}
