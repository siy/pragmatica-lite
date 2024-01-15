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
public interface ConfigFormatReader {
    /**
     * Read configuration values from string.
     */
    Result<StringMap> read(String content);

    /**
     * List of file extensions recognized by reader. Empty if not applicable.
     */
    List<String> supportedExtensions();

    static Stream<Tuple2<String, ConfigFormatReader>> readers() {
        return FormatReaders.INSTANCE.readers.stream();
    }

    enum FormatReaders {
        INSTANCE;
        private final List<Tuple2<String, ConfigFormatReader>> readers;

        FormatReaders() {
            readers = ServiceLoader.load(ConfigFormatReader.class)
                                   .stream()
                                   .map(Provider::get)
                                   .flatMap(reader -> reader.supportedExtensions()
                                                            .stream()
                                                            .map(ext -> tuple(ext, reader)))
                                   .toList();
        }
    }
}
