package org.pragmatica.http.server.config.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.error.WebError;
import org.pragmatica.lang.Result;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.pragmatica.lang.Result.lift;

public final class ObjectMapperSerializer implements Serializer {
    private final ObjectMapper objectMapper;

    private ObjectMapperSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static ObjectMapperSerializer withMapper(ObjectMapper mapper) {
        return new ObjectMapperSerializer(mapper);
    }

    public static ObjectMapperSerializer withDefault() {
        return new ObjectMapperSerializer(new ObjectMapper());
    }

    @Override
    public Result<ByteBuf> serialize(Object success) {
        return lift(
            e -> WebError.fromThrowable(HttpStatus.UNPROCESSABLE_ENTITY, e),
            () -> wrappedBuffer(objectMapper.writeValueAsBytes(success))
        );
    }

    @Override
    public <T> Result<T> deserialize(ByteBuf entity, TypeReference<T> literal) {
        return lift(
            e -> WebError.fromThrowable(HttpStatus.UNPROCESSABLE_ENTITY, e),
            () -> objectMapper.readValue(entity.array(), entity.arrayOffset(), entity.readableBytes(), literal)
        );
    }
}
