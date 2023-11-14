package org.pfj.http.server.config.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.buffer.ByteBuf;
import org.pragmatica.lang.Result;

public interface Serializer {
    Result<ByteBuf> serialize(Object success);
    <T> Result<T> deserialize(ByteBuf entity, TypeReference<T> literal);
}
