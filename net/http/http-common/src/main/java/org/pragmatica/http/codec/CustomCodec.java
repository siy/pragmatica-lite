package org.pragmatica.http.codec;

import io.netty.buffer.ByteBuf;
import org.pragmatica.http.ContentType;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

//TODO: abstract out the ByteBuf?
@SuppressWarnings("unused")
public interface CustomCodec {
    Result<ByteBuf> serialize(Object value, ContentType contentType);

    <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> token);
}
