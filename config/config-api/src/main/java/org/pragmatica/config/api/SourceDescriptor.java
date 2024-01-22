package org.pragmatica.config.api;

import org.pragmatica.config.provider.CommandLineProvider;
import org.pragmatica.config.provider.EnvironmentProvider;
import org.pragmatica.config.provider.SystemPropertiesProvider;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Descriptors for configuration sources. Note that sources which deal with files (e.g. {@link FileSourceDescriptor.File} and
 * {@link FileSourceDescriptor.Classpath}) should not specify extension. The list of extensions is determined by available
 * {@link FormatReader}s.
 * <p>
 * Error which happen during loading of the content, do not cause a failure of the entire configuration loading process. The error is logged and
 * process continues with next combination of source and file extension.
 */
public sealed interface SourceDescriptor {
    void load(Store store);

    sealed interface FileSourceDescriptor extends SourceDescriptor {
        record File(String path) implements FileSourceDescriptor {
            private static final Logger log = LoggerFactory.getLogger(File.class);

            @Override
            public void load(Store store) {
                FormatReader.readers()
                            .map(tuple -> tuple.map((ext, reader) -> readFile(ext, path())
                                      .onSuccessRun(() -> log.debug(STR."File \{path}.\{ext} successfully read"))
                                      .onFailure(cause -> log.debug(STR."Failed to read file \{path}.\{ext}: \{cause}", cause))
                                      .flatMap(reader::read)))
                            .forEach(result -> result.onSuccess(store::append));
            }

            static Result<String> readFile(String extension, String path) {
                return Result.lift(ConfigError::ioError, () -> Files.readString(Path.of(STR."\{path}.\{extension}")));
            }
        }

        record Classpath(String path) implements FileSourceDescriptor {
            private static final Logger log = LoggerFactory.getLogger(Classpath.class);

            @Override
            public void load(Store store) {
                FormatReader.readers()
                            .map(tuple -> tuple.map((ext, reader) -> loadFromClasspath(ext, path())
                                      .onSuccessRun(() -> log.debug(STR."File \{path}.\{ext} successfully loaded via classpath"))
                                      .onFailure(cause -> log.debug(STR."Failed to load file \{path}.\{ext} via classpath: \{cause}", cause))
                                      .flatMap(reader::read)))
                            .forEach(result -> result.onSuccess(store::append));
            }

            static Result<String> loadFromClasspath(String extension, String path) {
                try (var input = SourceDescriptor.class.getResourceAsStream(STR."\{path}.\{extension}")) {
                    if (input == null) {
                        return Result.failure(new ConfigError.InputIsMissing(STR."File \{path}.\{extension} not found in classpath"));
                    }

                    return Result.success(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                } catch (Exception e) {
                    return Result.failure(Causes.fromThrowable(e));
                }
            }
        }

        //TODO: implement Url source
//        record Url(String url) implements FileSourceDescriptor {}
    }

    sealed interface EnvironmentSourceDescriptor extends SourceDescriptor {
        Logger log = LoggerFactory.getLogger(EnvironmentSourceDescriptor.class);

        DataProvider provider();

        @Override
        default void load(Store store) {
            provider().read()
                      .onSuccess(store::append)
                      .onFailure(cause -> log.debug(STR."Failed to load \{getClass().getSimpleName()}: \{cause}", cause));
        }

        record Environment() implements EnvironmentSourceDescriptor {
            @Override
            public DataProvider provider() {
                return EnvironmentProvider.INSTANCE;
            }
        }

        record SystemProperties() implements EnvironmentSourceDescriptor {
            @Override
            public DataProvider provider() {
                return SystemPropertiesProvider.INSTANCE;
            }
        }

        record CommandLine(String[] arguments) implements EnvironmentSourceDescriptor {
            @Override
            public CommandLineProvider provider() {
                return this::arguments;
            }
        }
    }
}
