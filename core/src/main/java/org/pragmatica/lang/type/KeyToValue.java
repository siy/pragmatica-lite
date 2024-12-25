package org.pragmatica.lang.type;

import org.pragmatica.lang.Result;

/**
 * Abstract API for key-value store, where keys are always strings.
 */
public interface KeyToValue {
    <T> Result<T> get(String prefix, String key, TypeToken<T> typeToken);

    default String prependPrefix(String prefix, String key) {
        return prefix.isEmpty() ? key : prefix + "." + key;
    }
}
