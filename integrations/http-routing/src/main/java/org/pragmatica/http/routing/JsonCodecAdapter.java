/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.http.routing;

import org.pragmatica.json.JsonMapper;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

/// Adapter that wraps JsonMapper to implement JsonCodec interface.
/// Bridges between Jackson-based JSON serialization and Netty ByteBuf.
public record JsonCodecAdapter(JsonMapper mapper) implements JsonCodec {
    /// Creates a JsonCodec adapter for the given JsonMapper.
    ///
    /// @param mapper The JsonMapper instance to wrap
    /// @return JsonCodec implementation
    public static JsonCodec forMapper(JsonMapper mapper) {
        return new JsonCodecAdapter(mapper);
    }

    /// Creates a JsonCodec using the default JsonMapper configuration.
    ///
    /// @return JsonCodec implementation with default settings
    public static JsonCodec defaultCodec() {
        return new JsonCodecAdapter(JsonMapper.jsonMapper()
                                              .withPragmaticaTypes()
                                              .configure(builder -> builder.changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY)
                                                                                                                       .withContentInclusion(JsonInclude.Include.NON_EMPTY)))
                                              .build());
    }

    @Override
    public Result<ByteBuf> serialize(Object value) {
        return mapper.writeAsBytes(value)
                     .map(bytes -> Unpooled.wrappedBuffer(bytes))
                     .mapError(cause -> CodecError.serializationFailed(cause.message(),
                                                                       cause));
    }

    @Override
    public <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> token) {
        return deserialize(ByteBufUtil.getBytes(entity), token);
    }

    @Override
    public <T> Result<T> deserialize(String json, TypeToken<T> token) {
        return mapper.readString(json, token)
                     .mapError(cause -> CodecError.deserializationFailed(cause.message(),
                                                                         cause));
    }

    @Override
    public <T> Result<T> deserialize(byte[] bytes, TypeToken<T> token) {
        return mapper.readBytes(bytes, token)
                     .mapError(cause -> CodecError.deserializationFailed(cause.message(),
                                                                         cause));
    }
}
