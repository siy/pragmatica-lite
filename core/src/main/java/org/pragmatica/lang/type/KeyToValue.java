package org.pragmatica.lang.type;

import org.pragmatica.lang.Result;

/**
 * Abstract API for key-value store, where keys are always strings.
 */
public interface KeyToValue {
    <T> Result<T> get(String key, TypeToken<T> typeToken);
}
