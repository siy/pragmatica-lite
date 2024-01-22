package org.pragmatica.config.api;

import org.pragmatica.lang.Result;

public interface DataProvider {
    Result<StringMap> read();
}
