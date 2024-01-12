package org.pragmatica.config.api;

import org.pragmatica.lang.Result;

public interface ConfigDataProvider {
    Result<StringMap> read();
}
