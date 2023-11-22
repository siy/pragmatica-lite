package org.pragmatica.http.server.config.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.buffer.ByteBuf;
import org.pragmatica.lang.Result;

//TODO: externalize implementations via service loader
public interface Serializer {
    Result<ByteBuf> serialize(Object success);
    <T> Result<T> deserialize(ByteBuf entity, TypeReference<T> literal);
}
