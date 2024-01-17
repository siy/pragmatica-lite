package org.pragmatica.http.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.pragmatica.http.ContentType;
import org.pragmatica.http.codec.CodecError.DecodingError;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

//TODO: abstract out the ByteBuf?
@SuppressWarnings("unused")
public interface CustomCodec {
    Result<ByteBuf> serialize(Object value, ContentType contentType);

    <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> token);

    static CustomCodec empty() {
        return new CustomCodec() {
            @Override
            public Result<ByteBuf> serialize(Object value, ContentType contentType) {
                return Result.success(Unpooled.EMPTY_BUFFER);
            }

            @Override
            public <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> token) {
                return new DecodingError("Custom codec is not configured").result();
            }
        };
    }
}
