package org.pragmatica.http.server.impl;

import org.pragmatica.codec.json.JsonCodecFactory;
import org.pragmatica.http.codec.CustomCodec;
import org.pragmatica.http.codec.JsonCodec;
import org.pragmatica.http.server.HttpServerConfig;

public record ContextConfig(JsonCodec jsonCodec, CustomCodec customCodec) {
    public static ContextConfig fromHttpServerConfig(HttpServerConfig config) {
        return new ContextConfig(config.jsonCodec().or(JsonCodecFactory.defaultFactory()::withDefaultConfiguration),
                                 config.customCodec().or(CustomCodec::empty));
    }
}
