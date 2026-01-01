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

import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

import static org.pragmatica.lang.Result.success;

/// Jackson deserializer for Result<T> types.
/// Expects JSON in format: {"success": true, "value": <T>} or {"success": false, "error": {"message": "...", "type": "..."}}
public class ResultDeserializer extends ValueDeserializer<Result< ? >> {
    private final JavaType valueType;
    private final ValueDeserializer<Object> valueDeserializer;

    public ResultDeserializer() {
        this(null, null);
    }

    private ResultDeserializer(JavaType valueType, ValueDeserializer<Object> valueDeserializer) {
        this.valueType = valueType;
        this.valueDeserializer = valueDeserializer;
    }

    @Override
    public Result< ? > deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        if (p.currentToken() != tools.jackson.core.JsonToken.START_OBJECT) {
            throw new JacksonException("Expected START_OBJECT token") {};
        }
        Boolean isSuccess = null;
        Object value = null;
        String errorMessage = null;
        while (p.nextToken() != tools.jackson.core.JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();
            switch (fieldName) {
                case "success" -> isSuccess = p.getBooleanValue();
                case "value" -> {
                    if (valueDeserializer != null) {
                        value = valueDeserializer.deserialize(p, ctxt);
                    } else if (valueType != null) {
                        value = ctxt.readValue(p, valueType);
                    } else {
                        value = p.readValueAs(Object.class);
                    }
                }
                case "error" -> {
                    while (p.nextToken() != tools.jackson.core.JsonToken.END_OBJECT) {
                        String errorField = p.currentName();
                        p.nextToken();
                        if ("message".equals(errorField)) {
                            errorMessage = p.getString();
                        }
                    }
                }
            }
        }
        if (isSuccess == null) {
            throw new JacksonException("Missing 'success' field in Result JSON") {};
        }
        if (isSuccess) {
            return success(value);
        } else {
            return DeserializedCause.deserializedCause(errorMessage != null
                                                       ? errorMessage
                                                       : "Unknown error")
                                    .result();
        }
    }

    @Override
    public ValueDeserializer< ? > createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (property == null) {
            return this;
        }
        JavaType type = property.getType();
        if (type.hasContentType()) {
            JavaType contentType = type.getContentType();
            ValueDeserializer<Object> deser = ctxt.findContextualValueDeserializer(contentType, property);
            return new ResultDeserializer(contentType, deser);
        }
        return this;
    }
}
