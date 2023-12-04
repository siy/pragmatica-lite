package org.pragmatica.codec.json;

import io.netty.buffer.ByteBuf;
import org.pragmatica.lang.Result;

//TODO: abstract out the ByteBuf
public interface JsonCodec {
    Result<ByteBuf> serialize(Object success);

    <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> token);
}
