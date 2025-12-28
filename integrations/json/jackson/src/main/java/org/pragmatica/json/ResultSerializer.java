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

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;

/// Jackson serializer for Result<T> types.
/// Serializes Result as: {"success": true, "value": <T>} or {"success": false, "error": {"message": "...", "type": "..."}}
public class ResultSerializer extends ValueSerializer<Result< ? >> {
    private final JavaType valueType;
    private final ValueSerializer<Object> valueSerializer;

    public ResultSerializer() {
        this(null, null);
    }

    private ResultSerializer(JavaType valueType, ValueSerializer<Object> valueSerializer) {
        this.valueType = valueType;
        this.valueSerializer = valueSerializer;
    }

    @Override
    public void serialize(Result< ? > value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeStartObject();
        switch (value) {
            case Result.Success< ? > success -> {
                gen.writeBooleanProperty("success", true);
                gen.writeName("value");
                if (valueSerializer != null) {
                    valueSerializer.serialize(success.value(), gen, provider);
                }else {
                    gen.writePOJO(success.value());
                }
            }
            case Result.Failure< ? > failure -> {
                gen.writeBooleanProperty("success", false);
                gen.writeName("error");
                gen.writeStartObject();
                gen.writeStringProperty("message",
                                        failure.cause()
                                               .message());
                gen.writeStringProperty("type",
                                        failure.cause()
                                               .getClass()
                                               .getSimpleName());
                gen.writeEndObject();
            }
        }
        gen.writeEndObject();
    }

    @Override
    public ValueSerializer< ? > createContextual(SerializationContext prov, BeanProperty property) {
        if (property == null) {
            return this;
        }
        JavaType type = property.getType();
        if (type.hasContentType()) {
            JavaType contentType = type.getContentType();
            ValueSerializer<Object> ser = prov.findValueSerializer(contentType);
            return new ResultSerializer(contentType, ser);
        }
        return this;
    }

    @Override
    public void serializeWithType(Result< ? > value,
                                  JsonGenerator gen,
                                  SerializationContext provider,
                                  TypeSerializer typeSer) throws JacksonException {
        serialize(value, gen, provider);
    }
}
