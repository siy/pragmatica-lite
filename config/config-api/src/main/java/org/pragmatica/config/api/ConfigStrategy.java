package org.pragmatica.config.api;

import org.pragmatica.config.api.SourceDescriptor.FileSourceDescriptor.Classpath;
import org.pragmatica.config.api.SourceDescriptor.FileSourceDescriptor.File;

import java.util.List;

import static org.pragmatica.config.api.SourceDescriptor.EnvironmentSourceDescriptor.*;

/**
 * Strategy for configuration sources defines order and source of the configuration data. Returned sources are loaded in order and sources loaded
 * later override values from sources loaded earlier. Use of strategy enables flexible adjustment of the configuration loading process.
 * <p>
 * The default strategy loads defaults for main components and then overrides them with values from application configuration files, starting from one
 * provided in classpath and then one provided as a file in the current directory. Finally, values from environment variables, system properties and
 * command line arguments are loaded. Such an order should be sufficient for most typical use cases.
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
