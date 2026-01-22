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

import org.pragmatica.lang.Result;
import org.pragmatica.lang.Verify;
import org.pragmatica.utility.IdGenerator;

/// Unique identifier for a command batch.
public record BatchId(String id) {
    public static Result<BatchId> batchId(String id) {
        return Verify.ensure(id, Verify.Is::notBlank)
                     .map(BatchId::new);
    }

    public static BatchId randomBatchId() {
        return new BatchId(IdGenerator.generate("batch"));
    }

    public static BatchId emptyBatchId() {
        return new BatchId("empty");
    }
}
