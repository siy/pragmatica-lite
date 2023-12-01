package org.pragmatica.codec.json;

import io.netty.buffer.ByteBuf;
import org.pragmatica.lang.Result;

public interface JsonCodec<C extends JsonCodecConfiguration> {
    Result<ByteBuf> serialize(Object success);

    <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> token);
}
