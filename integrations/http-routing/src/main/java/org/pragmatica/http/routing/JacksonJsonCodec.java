package org.pragmatica.http.routing;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.pragmatica.lang.Result.lift;

public interface JacksonJsonCodec extends JsonCodec {
    static JacksonJsonCodec forMapper(JsonMapper jsonMapper) {
        record jacksonJsonCodec(JsonMapper jsonMapper) implements JacksonJsonCodec {
            @Override
            public Result<ByteBuf> serialize(Object value) {
                return lift(CodecError::fromSerializationThrowable,
                            () -> wrappedBuffer(jsonMapper.writeValueAsBytes(value)));
            }

            @Override
            public <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> token) {
                var typeRef = toTypeReference(token);
                return lift(CodecError::fromDeserializationThrowable,
                            () -> jsonMapper.readValue(ByteBufUtil.getBytes(entity), typeRef));
            }

            @Override
            public <T> Result<T> deserialize(String json, TypeToken<T> token) {
                var typeRef = toTypeReference(token);
                return lift(CodecError::fromDeserializationThrowable, () -> jsonMapper.readValue(json, typeRef));
            }

            @Override
            public <T> Result<T> deserialize(byte[] bytes, TypeToken<T> token) {
                var typeRef = toTypeReference(token);
                return lift(CodecError::fromDeserializationThrowable, () -> jsonMapper.readValue(bytes, typeRef));
            }

            private static <T> TypeReference<T> toTypeReference(TypeToken<T> typeToken) {
                return new TypeReference<>() {
                    @Override
                    public Type getType() {
                        return typeToken.token();
                    }
                };
            }
        }
        return new jacksonJsonCodec(jsonMapper);
    }

    static JacksonJsonCodec defaultCodec() {
        return forMapper(JsonMapper.builder()
                                   .build());
    }
}
