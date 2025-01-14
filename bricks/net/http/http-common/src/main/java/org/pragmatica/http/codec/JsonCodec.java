package org.pragmatica.http.codec;

import io.netty.buffer.ByteBuf;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

//TODO: abstract out the ByteBuf?
public interface JsonCodec {
    Result<ByteBuf> serialize(Object value);

    <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> token);
}
