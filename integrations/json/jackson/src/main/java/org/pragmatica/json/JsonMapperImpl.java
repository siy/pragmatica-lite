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

package org.pragmatica.json;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper.Builder;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.pragmatica.lang.Result.lift;

/// Implementation of JsonMapper interface wrapping Jackson 3.0 JsonMapper.
record JsonMapperImpl(tools.jackson.databind.json.JsonMapper mapper) implements JsonMapper {
    @Override
    public <T> Result<String> writeAsString(T value) {
        return lift(JsonError::fromException, () -> mapper.writeValueAsString(value));
    }

    @Override
    public <T> Result<byte[]> writeAsBytes(T value) {
        return lift(JsonError::fromException, () -> mapper.writeValueAsBytes(value));
    }

    @Override
    public <T> Result<T> readString(String json, Class<T> type) {
        return lift(JsonError::fromException, () -> mapper.readValue(json, type));
    }

    @Override
    public <T> Result<T> readBytes(byte[] json, Class<T> type) {
        return lift(JsonError::fromException, () -> mapper.readValue(json, type));
    }

    @Override
    public <T> Result<T> readString(String json, TypeReference<T> typeRef) {
        return lift(JsonError::fromException, () -> mapper.readValue(json, typeRef));
    }

    @Override
    public <T> Result<T> readBytes(byte[] json, TypeReference<T> typeRef) {
        return lift(JsonError::fromException, () -> mapper.readValue(json, typeRef));
    }

    @Override
    public <T> Result<T> readString(String json, TypeToken<T> typeToken) {
        return readString(json, toTypeReference(typeToken));
    }

    @Override
    public <T> Result<T> readBytes(byte[] json, TypeToken<T> typeToken) {
        return readBytes(json, toTypeReference(typeToken));
    }

    /// Converts TypeToken to Jackson TypeReference.
    private static <T> TypeReference<T> toTypeReference(TypeToken<T> typeToken) {
        return new TypeReference<>() {
            @Override
            public Type getType() {
                return typeToken.token();
            }
        };
    }

    static final class BuilderImpl implements JsonMapperBuilder {
        private final List<JacksonModule> modules = new ArrayList<>();
        private final List<Consumer<Builder>> configurators = new ArrayList<>();

        @Override
        public JsonMapperBuilder withPragmaticaTypes() {
            return withModule(new PragmaticaModule());
        }

        @Override
        public JsonMapperBuilder withModule(JacksonModule module) {
            modules.add(module);
            return this;
        }

        @Override
        public JsonMapperBuilder configure(Consumer<Builder> configurator) {
            configurators.add(configurator);
            return this;
        }

        @Override
        public JsonMapper build() {
            var builder = tools.jackson.databind.json.JsonMapper.builder();

            // Register modules
            modules.forEach(builder::addModule);

            // Apply configurators
            configurators.forEach(c -> c.accept(builder));

            return new JsonMapperImpl(builder.build());
        }
    }
}
