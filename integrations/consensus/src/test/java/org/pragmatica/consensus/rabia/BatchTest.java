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
import org.pragmatica.consensus.Command;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BatchTest {

    record TestCommand(String value) implements Command {}

    @Test
    void batch_creates_batch_with_commands() {
        var commands = List.of(new TestCommand("a"), new TestCommand("b"));

        var batch = Batch.batch(commands);

        assertThat(batch.commands()).hasSize(2);
        assertThat(batch.id().id()).startsWith("batch-");
        assertThat(batch.correlationIds().getFirst().id()).startsWith("xref-");
        assertThat(batch.isNotEmpty()).isTrue();
    }

    @Test
    void emptyBatch_creates_empty_batch() {
        var batch = Batch.<TestCommand>emptyBatch();

        assertThat(batch.commands()).isEmpty();
        assertThat(batch.id().id()).isEqualTo("empty");
        assertThat(batch.isNotEmpty()).isFalse();
    }

    @Test
    void batches_are_comparable_by_timestamp() {
        var batch1 = Batch.batch(List.of(new TestCommand("a")));
        var batch2 = Batch.batch(List.of(new TestCommand("b")));

        // batch2 created after batch1, so batch1 < batch2
        assertThat(batch1.compareTo(batch2)).isLessThanOrEqualTo(0);
    }

    @Test
    void batch_hashCode_uses_id() {
        var commands = List.of(new TestCommand("a"));
        var batch = Batch.batch(commands);

        assertThat(batch.hashCode()).isEqualTo(batch.id().hashCode());
    }
}
