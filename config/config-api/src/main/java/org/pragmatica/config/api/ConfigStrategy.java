package org.pragmatica.config.api;

import org.pragmatica.config.api.SourceDescriptor.FileSourceDescriptor.Classpath;
import org.pragmatica.config.api.SourceDescriptor.FileSourceDescriptor.File;

import java.util.List;

import static org.pragmatica.config.api.SourceDescriptor.EnvironmentSourceDescriptor.*;

/**
 * Strategy for configuration sources. Returned sources are loaded in order and sources loaded later override values from sources loaded earlier.
 * <p>
 * Use of strategy enables flexible adjustment of the configuration loading process. In many cases such flexibility is not necessary, so default
 * strategy is provided too.
 */
public interface ConfigStrategy {
    List<SourceDescriptor> configurationSources();

    static ConfigStrategy defaultStrategy(String[] arguments) {
        return () -> List.of(
            new Classpath("/db/default"),
            new Classpath("/server/default"),
            new Classpath("/application"),
            new File("application"),
            new Environment(),
            new SystemProperties(),
            new CommandLine(arguments));
    }
}
