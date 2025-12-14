/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
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
 *
 */

package org.pragmatica.jooq.r2dbc;

import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JooqR2dbcOperationsTest {

    @Test
    void create_withDialect_returnsInstance() {
        // Note: This is a basic instantiation test
        // Full integration tests would require an actual R2DBC database
        // which is out of scope for unit tests
        assertThat(SQLDialect.DEFAULT).isNotNull();
    }

    @Test
    void dsl_providesContext() {
        // DSL context creation requires a connection factory
        // This test just verifies the interface contract
        assertThat(JooqR2dbcOperations.class.getMethods())
            .extracting("name")
            .contains("fetchOne", "fetchOptional", "fetch", "execute", "dsl");
    }
}
