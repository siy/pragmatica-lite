package org.pfj.http.server.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import org.pfj.http.server.error.WebError;
import org.pfj.lang.Result;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.pfj.http.server.error.CompoundCause.fromThrowable;
import static org.pfj.lang.Result.lift;

public final class DefaultSerializer implements Serializer {
    private final ObjectMapper objectMapper;

    private DefaultSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static DefaultSerializer withMapper(ObjectMapper mapper) {
        return new DefaultSerializer(mapper);
    }

    public static DefaultSerializer withDefault() {
        return new DefaultSerializer(new ObjectMapper());
    }

    @Override
    public Result<ByteBuf> serialize(Object success) {
        return lift(
            e -> fromThrowable(WebError.UNPROCESSABLE_ENTITY, e),
            () -> wrappedBuffer(objectMapper.writeValueAsBytes(success))
        );
    }

    @Override
    public <T> Result<T> deserialize(ByteBuf entity, TypeReference<T> literal) {
        return lift(
            e -> fromThrowable(WebError.UNPROCESSABLE_ENTITY, e),
            () -> objectMapper.readValue(entity.array(), entity.arrayOffset(), entity.readableBytes(), literal)
        );
    }
}
