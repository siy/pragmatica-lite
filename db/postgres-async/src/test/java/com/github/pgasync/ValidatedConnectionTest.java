/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pgasync;

import com.github.pgasync.net.Connectible;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;

import java.util.function.Consumer;

/**
 * Tests for validated connections.
 *
 * @author Marat Gainullin
 */
@Tag("Slow")
public class ValidatedConnectionTest {

    @Rule
    public final DatabaseRule dbr = DatabaseRule.defaultConfiguration();

    private void withSource(Connectible source, Consumer<Connectible> action) {
        try {
            action.accept(source);
        } finally {
            source.close().await();
        }
    }

    private void withPlain(String clause, Consumer<Connectible> action) {
        withSource(dbr.builder
                       .validationQuery(clause)
                       .plain(),
                   action);
    }

    private void withPool(String clause, Consumer<Connectible> action) {
        withSource(dbr.builder
                       .validationQuery(clause)
                       .pool(),
                   action
        );
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldReturnValidPlainConnection() {
        withPlain("Select 89", plain -> {
            var conn = plain.connection().await().unwrap();
            conn.close().await();
        });
    }

    @Test
    public void shouldNotReturnInvalidPlainConnection() {
        withPlain("Selec t 89", plain -> plain.connection().await().onSuccess(Assert::fail));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldReturnValidPooledConnection() {
        withPool("Select 89", source -> {
            var conn = source.connection().await().unwrap();
            conn.close().await().onFailure(Assert::fail);
        });
    }

    @Test
    public void shouldNotReturnInvalidPooledConnection() {
        withPool("Selec t 89", source -> source.connection().await().onSuccess(Assert::fail));
    }
}
