package org.pragmatica.http.routing;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import io.netty.buffer.ByteBuf;

public interface JsonCodec {
    Result<ByteBuf> serialize(Object value);

    <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> token);

    <T> Result<T> deserialize(String json, TypeToken<T> token);

    <T> Result<T> deserialize(byte[] bytes, TypeToken<T> token);

    default <T> Result<T> deserialize(ByteBuf entity, Class<T> type) {
        return deserialize(entity, TypeToken.of(type));
    }

    default <T> Result<T> deserialize(String json, Class<T> type) {
        return deserialize(json, TypeToken.of(type));
    }

    default <T> Result<T> deserialize(byte[] bytes, Class<T> type) {
        return deserialize(bytes, TypeToken.of(type));
    }
}
