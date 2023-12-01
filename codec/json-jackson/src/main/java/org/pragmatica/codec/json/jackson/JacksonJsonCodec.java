package org.pragmatica.codec.json.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import org.pragmatica.codec.json.CodecError;
import org.pragmatica.codec.json.JsonCodec;
import org.pragmatica.codec.json.TypeToken;
import org.pragmatica.lang.Result;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.pragmatica.lang.Result.lift;

public interface JacksonJsonCodec extends JsonCodec<JacksonJsonCodecConfiguration> {
    static JacksonJsonCodec forMapper(ObjectMapper objectMapper) {
        record jacksonJsonCodec(ObjectMapper objectMapper) implements JacksonJsonCodec {
            @Override
            public Result<ByteBuf> serialize(Object success) {
                return lift(
                    CodecError::fromCodingThrowable,
                    () -> wrappedBuffer(objectMapper.writeValueAsBytes(success))
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
