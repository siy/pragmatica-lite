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
 * {@link ConfigFormatReader}s.
 * <p>
 * Note that error which happens during loading of the content, does not cause failure of the configuration loading process. The error is logged and
 * process continues with next source (or same source with next file extension, see {@link ConfigStrategy} for more details).
 */
public sealed interface SourceDescriptor {
    void load(ConfigStore store);

    sealed interface FileSourceDescriptor extends SourceDescriptor {
        record File(String path) implements FileSourceDescriptor {
            private static final Logger log = LoggerFactory.getLogger(File.class);

            @Override
            public void load(ConfigStore store) {
                ConfigFormatReader.readers()
                                  .map(tuple -> tuple.map((ext, reader) -> readFile(ext, path())
                                      .onFailure(cause -> log.debug(STR."Failed to read file \{path}.\{ext}: \{cause}", cause))
                                      .flatMap(reader::read)))
                                  .forEach(result -> result.onSuccess(store::append));
            }

            static Result<String> readFile(String extension, String path) {
                return Result.lift(Causes::fromThrowable, () -> Files.readString(Path.of(STR."\{path}.\{extension}")));
            }
        }

        record Classpath(String path) implements FileSourceDescriptor {
            private static final Logger log = LoggerFactory.getLogger(Classpath.class);

            @Override
            public void load(ConfigStore store) {
                ConfigFormatReader.readers()
                                  .map(tuple -> tuple.map((ext, reader) -> loadFromClasspath(ext, path())
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

        //TODO: implement Url
//        record Url(String url) implements FileSourceDescriptor {}
    }

    sealed interface EnvironmentSourceDescriptor extends SourceDescriptor {
        Logger log = LoggerFactory.getLogger(EnvironmentSourceDescriptor.class);

        ConfigDataProvider provider();

        @Override
        default void load(ConfigStore store) {
            provider().read()
                      .onSuccess(store::append)
                      .onFailure(cause -> log.debug(STR."Failed to load \{getClass().getSimpleName()}: \{cause}", cause));
        }

        record Environment() implements EnvironmentSourceDescriptor {
            @Override
            public ConfigDataProvider provider() {
                return EnvironmentProvider.INSTANCE;
            }
        }

        record SystemProperties() implements EnvironmentSourceDescriptor {
            @Override
            public ConfigDataProvider provider() {
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
