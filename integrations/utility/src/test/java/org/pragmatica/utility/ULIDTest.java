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

package org.pragmatica.utility;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ULIDTest {

    @Test
    void randomULID_generates_26_character_string() {
        var ulid = ULID.randomULID();

        assertThat(ulid.encoded()).hasSize(ULID.ULID_LENGTH);
    }

    @Test
    void randomULID_generates_unique_values() {
        var ulid1 = ULID.randomULID();
        var ulid2 = ULID.randomULID();

        assertThat(ulid1).isNotEqualTo(ulid2);
    }

    @Test
    void ulid_is_comparable() {
        var ulid1 = ULID.randomULID();
        var ulid2 = ULID.randomULID();

        // ulid2 created after ulid1, so ulid1 < ulid2
        assertThat(ulid1.compareTo(ulid2)).isLessThan(0);
    }

    @Test
    void ulid_has_entropy() {
        var ulid = ULID.randomULID();

        assertThat(ulid.entropy()).hasSize(10);
    }

    @Test
    void ulid_has_timestamp() {
        var ulid = ULID.randomULID();

        assertThat(ulid.timestamp()).isPositive();
    }
}
