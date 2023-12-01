package org.pragmatica.codec.json.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.auto.service.AutoService;
import io.netty.buffer.ByteBuf;
import org.pragmatica.codec.json.Serializer;
import org.pragmatica.codec.json.SerializerConfiguration;
import org.pragmatica.codec.json.SerializerError;
import org.pragmatica.codec.json.TypeToken;
import org.pragmatica.lang.Result;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.pragmatica.lang.Result.lift;

@AutoService(Serializer.class)
public final class JacksonSerializer implements Serializer {
    private final ObjectMapper objectMapper;

    private JacksonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static JacksonSerializer withMapper(ObjectMapper mapper) {
        return new JacksonSerializer(mapper);
    }

    public static JacksonSerializer withDefault() {
        return new JacksonSerializer(new ObjectMapper());
    }

    @Override
    public Result<ByteBuf> serialize(Object success) {
        return lift(
            SerializerError::fromSerializationThrowable,
            () -> wrappedBuffer(objectMapper.writeValueAsBytes(success))
        );
    }

    @Override
    public <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> literal) {
        JavaType javaType = objectMapper.constructType(literal.token());
        return lift(
            SerializerError::fromDeserializationThrowable,
            () -> objectMapper.readValue(entity.array(), entity.arrayOffset(), entity.readableBytes(), javaType)
        );
    }

    @Override
    public Class<? extends SerializerConfiguration> configClass() {
        return JacksonSerializerConfig.class;
    }
}
