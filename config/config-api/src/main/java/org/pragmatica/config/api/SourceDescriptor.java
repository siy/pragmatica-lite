package org.pragmatica.config.api;

import org.pragmatica.config.provider.CommandLineProvider;
import org.pragmatica.config.provider.EnvironmentProvider;
import org.pragmatica.config.provider.SystemPropertiesProvider;

/**
 * Descriptors for configuration sources. Note that sources which deal with files (e.g. {@link FileSourceDescriptor.File}
 * and {@link FileSourceDescriptor.Classpath}) should not specify extension. The list of extensions is determined by available {@link ConfigFormatReader}s.
 * <p>
 * Note that error which happens during loading of the content, does not cause failure of the configuration loading process. The error is logged and
 * process continues with next source (or same source with next file extension, see {@link ConfigStrategy} for more details).
 */
public sealed interface SourceDescriptor {

    sealed interface FileSourceDescriptor extends SourceDescriptor {
        record File(String path) implements FileSourceDescriptor {}

        record Classpath(String path) implements FileSourceDescriptor {}

        //TODO: implement Url
        record Url(String url) implements FileSourceDescriptor {}
    }

    sealed interface EnvironmentSourceDescriptor extends SourceDescriptor {
        ConfigDataProvider provider();
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