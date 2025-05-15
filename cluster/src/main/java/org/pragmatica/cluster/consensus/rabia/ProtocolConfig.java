package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.lang.io.TimeSpan;

import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/**
 * Configuration for the Rabia consensus engine.
 */
public record ProtocolConfig(
        TimeSpan cleanupInterval,
        TimeSpan syncRetryInterval,
        long removeOlderThanPhases
) {
    /// Creates a default (production) configuration.
    public static ProtocolConfig defaultConfig() {
        return new ProtocolConfig(timeSpan(60).seconds(),
                                  timeSpan(5).seconds(),
                                  100);
    }

    /// Creates a test configuration.
    public static ProtocolConfig testConfig() {
        return new ProtocolConfig(timeSpan(60).seconds(),
                                  timeSpan(100).millis(),
                                  100);
    }
}
