package org.pragmatica.codec.json.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import org.pragmatica.http.codec.CodecError;
import org.pragmatica.http.codec.JsonCodec;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.pragmatica.lang.Result.lift;

@SuppressWarnings("unused")
public interface JacksonJsonCodec extends JsonCodec {
    static JacksonJsonCodec forMapper(ObjectMapper objectMapper) {
        record jacksonJsonCodec(ObjectMapper objectMapper) implements JacksonJsonCodec {
            @Override
            public Result<ByteBuf> serialize(Object value) {
                return lift(
                    CodecError::fromCodingThrowable,
                    () -> wrappedBuffer(objectMapper.writeValueAsBytes(value))
                );
            }

            @Override
            public <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> literal) {
                JavaType javaType = objectMapper.constructType(literal.token());
                return lift(
                    CodecError::fromDecodingThrowable,
                    () -> objectMapper.readValue(entity.array(), entity.arrayOffset(), entity.readableBytes(), javaType)
                );
            }
        }
        return new jacksonJsonCodec(objectMapper);
    }
}
