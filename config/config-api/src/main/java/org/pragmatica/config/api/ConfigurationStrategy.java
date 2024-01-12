package org.pragmatica.config.api;

import java.util.List;

/**
 * Strategy for configuration sources. Returned sources are loaded in order and sources loaded later override values from sources loaded earlier.
 * <p>
 * Use of strategy enables flexible adjustment of the configuration loading process. In many cases such flexibility is not necessary, so default
 * strategy is provided too.
 */
public interface ConfigurationStrategy {
    List<SourceDescriptor> orderedSources();

    static ConfigurationStrategy defaultStrategy() {
        return () -> List.of(new File("application"),
                                new Classpath("application"),
            new SourceDescriptor.Environment(),
                             new SourceDescriptor.SystemProperties(),
                             new SourceDescriptor.CommandLine());
    }
}
