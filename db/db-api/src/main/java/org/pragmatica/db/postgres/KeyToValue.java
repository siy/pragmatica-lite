package org.pragmatica.db.postgres;

import org.pragmatica.http.codec.TypeToken;
import org.pragmatica.lang.Result;

/**
 * Abstract API for key-value store, where keys are always strings and values are converted from string on demand.
 */
public interface KeyToValue {
    <T> Result<T> get(String key, TypeToken<T> typeToken);
}
