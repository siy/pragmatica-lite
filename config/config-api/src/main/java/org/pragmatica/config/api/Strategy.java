package org.pragmatica.config.api;

import org.pragmatica.config.api.SourceDescriptor.FileSourceDescriptor.Classpath;
import org.pragmatica.config.api.SourceDescriptor.FileSourceDescriptor.File;

import java.util.List;
import java.util.stream.Stream;

import static org.pragmatica.config.api.SourceDescriptor.EnvironmentSourceDescriptor.*;

/**
 * Strategy for configuration sources defines order and source of the configuration data. Returned sources are loaded in order and sources loaded
 * later override values from sources loaded earlier. Use of strategy enables flexible adjustment of the configuration loading process.
 * <p>
 * The {@link #defaultStrategy} strategy loads defaults for main components and then overrides them with values from application configuration files,
 * starting from one provided in classpath and then one provided as a file in the current directory. Finally, values from environment variables and
 * system properties are loaded. Such an order should be sufficient for most typical use cases.
 * <p>
 * The {@link #defaultWithCommandLine} strategy loads everything from {@link #defaultStrategy()} and then adds command line parameters on top of them.
 */
public interface Strategy {
    List<SourceDescriptor> configurationSources();

    default Strategy with(SourceDescriptor source) {
        var newList = Stream.concat(configurationSources().stream(),
                                    Stream.of(source))
                            .toList();

        return () -> newList;
    }

    static Strategy defaultStrategy() {
        return () -> List.of(
            new Classpath("/db/default"),
            new Classpath("/server/default"),
            new Classpath("/resolver/default"),
            new Classpath("/application"),
            new File("application"),
            new Environment(),
            new SystemProperties());
    }

    static Strategy defaultWithCommandLine(String[] arguments) {
        return defaultStrategy().with(new CommandLine(arguments));
    }
}
