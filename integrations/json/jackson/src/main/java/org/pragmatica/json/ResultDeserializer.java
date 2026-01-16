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

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

import static org.pragmatica.lang.Option.option;
import static org.pragmatica.lang.Result.success;

/// Jackson deserializer for Result<T> types.
/// Expects JSON in format: {"success": true, "value": <T>} or {"success": false, "error": {"message": "...", "type": "..."}}
public class ResultDeserializer extends ValueDeserializer<Result<?>> {
    private final Option<JavaType> valueType;
    private final Option<ValueDeserializer<Object>> valueDeserializer;

    public ResultDeserializer() {
        this(Option.none(), Option.none());
    }

    private ResultDeserializer(Option<JavaType> valueType, Option<ValueDeserializer<Object>> valueDeserializer) {
        this.valueType = valueType;
        this.valueDeserializer = valueDeserializer;
    }

    @Override
    public Result<?> deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        if (p.currentToken() != tools.jackson.core.JsonToken.START_OBJECT) {
            throw new JacksonException("Expected START_OBJECT token") {};
        }
        Option<Boolean> isSuccess = Option.none();
        Object value = null;
        Option<String> errorMessage = Option.none();
        while (p.nextToken() != tools.jackson.core.JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();
            switch (fieldName) {
                case "success" -> isSuccess = Option.some(p.getBooleanValue());
                case "value" -> value = deserializeValue(p, ctxt);
                case "error" -> {
                    while (p.nextToken() != tools.jackson.core.JsonToken.END_OBJECT) {
                        String errorField = p.currentName();
                        p.nextToken();
                        if ("message".equals(errorField)) {
                            errorMessage = Option.some(p.getString());
                        }
                    }
                }
            }
        }
        Object finalValue = value;
        var finalErrorMessage = errorMessage;
        var cause = Causes.cause("Missing 'success' field in Result JSON");
        return isSuccess.toResult(cause)
                        .flatMap(successFlag -> successFlag
                                                ? success(finalValue)
                                                : DeserializedCause.deserializedCause(finalErrorMessage.or("Unknown error"))
                                                                   .result());
    }

    private Object deserializeValue(JsonParser p, DeserializationContext ctxt) {
        return valueDeserializer.map(deser -> deserializeWith(deser, p, ctxt))
                                .orElse(() -> valueType.map(type -> readValue(ctxt, p, type)))
                                .or(() -> readValueAs(p));
    }

    private static Object deserializeWith(ValueDeserializer<Object> deser, JsonParser p, DeserializationContext ctxt) {
        try{
            return deser.deserialize(p, ctxt);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object readValue(DeserializationContext ctxt, JsonParser p, JavaType type) {
        try{
            return ctxt.readValue(p, type);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object readValueAs(JsonParser p) {
        try{
            return p.readValueAs(Object.class);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        return option(property).map(BeanProperty::getType)
                     .filter(JavaType::hasContentType)
                     .<ValueDeserializer<?>> map(type -> {
                                                     JavaType contentType = type.getContentType();
                                                     ValueDeserializer<Object> deser = ctxt.findContextualValueDeserializer(contentType,
                                                                                                                            property);
                                                     return new ResultDeserializer(option(contentType),
                                                                                   option(deser));
                                                 })
                     .or(this);
    }
}
