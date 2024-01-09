package org.pragmatica.config.api;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

/**
 * Abstract API for key-value store, where keys are always strings and values are converted from string on demand.
 */
public interface KeyToValue {
    <T> Result<T> get(String key, TypeToken<T> typeToken);
}
