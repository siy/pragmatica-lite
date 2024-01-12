package org.pragmatica.config.api;

/**
 * Descriptors for configuration sources. Note that sources which deal with files (e.g. {@link File} and {@link Classpath}) should not specify
 * extension. The list of extensions is defined dynamically by loading available {@link ConfigFormatReader}s.
 * <p>
 * Note that error which happens during loading of the content, does not cause failure of the configuration loading process. The error is logged and
 * process continues with next source (or same source with next file extension, see {@link ConfigurationStrategy} for more details).
 */
public sealed interface SourceDescriptor {

    sealed interface FileSourceDescriptor extends SourceDescriptor {
        record File(String path) implements FileSourceDescriptor {}

        record Classpath(String path) implements FileSourceDescriptor {}

        //TODO: implement Url
//        record Url(String url) implements FileBasedSourceDescriptor {}
    }

    sealed interface EnvironmentSourceDescriptor extends SourceDescriptor {
        record Environment() implements EnvironmentSourceDescriptor {}

        record SystemProperties() implements EnvironmentSourceDescriptor {}

        record CommandLine() implements EnvironmentSourceDescriptor {}
    }
}
