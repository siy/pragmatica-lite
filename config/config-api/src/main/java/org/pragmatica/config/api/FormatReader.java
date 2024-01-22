package org.pragmatica.config.api;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;

import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Stream;

import static org.pragmatica.lang.Tuple.tuple;

/**
 * Interface for configuration format readers (pluggable component).
 */
public interface FormatReader {
    /**
     * Read configuration values from string.
     */
    Result<StringMap> read(String content);

    /**
     * List of file extensions recognized by reader. Empty if not applicable.
     */
    List<String> supportedExtensions();

    static Stream<Tuple2<String, FormatReader>> readers() {
        return ServiceLoader.load(FormatReader.class)
                            .stream()
                            .map(Provider::get)
                            .flatMap(reader -> reader.supportedExtensions()
                                                     .stream()
                                                     .map(ext -> tuple(ext, reader)));
    }
}
