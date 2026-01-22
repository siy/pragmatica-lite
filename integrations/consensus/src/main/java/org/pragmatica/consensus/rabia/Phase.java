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
/// Represents a protocol phase as defined in the Rabia formal specification.
public record Phase(long value) implements Comparable<Phase> {
    public static final Phase ZERO = new Phase(0);

    public Phase {
        if (value < 0) {
            throw new IllegalArgumentException("Phase value must be non-negative: " + value);
        }
    }

    /// Creates a Phase from the given value.
    public static Phase phase(long value) {
        return new Phase(value);
    }

    /// Creates the successor phase to this phase.
    public Phase successor() {
        return new Phase(value + 1);
    }

    @Override
    public int compareTo(Phase other) {
        return Long.compare(this.value, other.value);
    }
}
