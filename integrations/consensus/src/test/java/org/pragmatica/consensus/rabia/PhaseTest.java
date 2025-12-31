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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhaseTest {

    @Test
    void zero_phase_has_value_zero() {
        assertThat(Phase.ZERO.value()).isZero();
    }

    @Test
    void successor_increments_phase() {
        var phase = new Phase(5);

        var next = phase.successor();

        assertThat(next.value()).isEqualTo(6);
    }

    @Test
    void phases_are_comparable() {
        var phase1 = new Phase(1);
        var phase2 = new Phase(2);
        var phase3 = new Phase(1);

        assertThat(phase1.compareTo(phase2)).isLessThan(0);
        assertThat(phase2.compareTo(phase1)).isGreaterThan(0);
        assertThat(phase1.compareTo(phase3)).isZero();
    }
}
