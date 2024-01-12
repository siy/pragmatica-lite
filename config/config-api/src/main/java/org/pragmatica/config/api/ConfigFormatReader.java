package org.pragmatica.config.api;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

import java.util.List;

/**
 * Interface for configuration format readers (pluggable component).
 */
public interface ConfigFormatReader {
    /**
     * Read configuration values as strings.
     * <p>
     * Readers which don't support reading from file (e.g. environment variables) will receive empty content. They should retrieve configuration
     * values by some other means (e.g. by reading environment variables).
     */
    Result<StringMap> read(Option<String> content);

    /**
     * List of file extensions recognized by reader. Empty if not applicable.
     */
    List<String> supportedExtensions();
}
