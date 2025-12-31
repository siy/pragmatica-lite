/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.consensus.rabia;

import org.pragmatica.lang.io.TimeSpan;

import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/**
 * Configuration for the Rabia consensus engine.
 */
public record ProtocolConfig(
        TimeSpan cleanupInterval,
        TimeSpan syncRetryInterval,
        long removeOlderThanPhases) {

    /**
     * Creates a default (production) configuration.
     */
    public static ProtocolConfig defaultConfig() {
        return new ProtocolConfig(
                timeSpan(60).seconds(),
                timeSpan(5).seconds(),
                100);
    }

    /**
     * Creates a test configuration with faster intervals.
     */
    public static ProtocolConfig testConfig() {
        return new ProtocolConfig(
                timeSpan(60).seconds(),
                timeSpan(100).millis(),
                100);
    }
}
