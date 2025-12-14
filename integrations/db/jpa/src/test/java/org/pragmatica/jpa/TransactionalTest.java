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

package org.pragmatica.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for Transactional aspect decorator.
///
/// Uses stubs instead of Mockito mocks due to lack of support for Java 25 bytecode in Mockito at the moment of writing.
/// See [EntityManagerStub] for the detailed explanation.
class TransactionalTest {

    @Nested
    class SuccessfulTransactions {
        @Test
        void withTransaction_commitsOnSuccess() {
            var calls = new ArrayList<String>();
            var em = createEntityManager(createTransaction(calls));

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (String input) -> Promise.success("result-" + input)
            );

            operation.apply("test")
                .await()
                .onFailure(cause -> fail("Should not fail: " + cause))
                .onSuccess(result -> assertEquals("result-test", result));

            assertEquals(List.of("begin", "commit"), calls);
        }

        @Test
        void withTransaction_Fn0_commitsOnSuccess() {
            var calls = new ArrayList<String>();
            var em = createEntityManager(createTransaction(calls));

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                () -> Promise.success("result")
            );

            operation.apply()
                .await()
                .onFailure(cause -> fail("Should not fail: " + cause))
                .onSuccess(result -> assertEquals("result", result));

            assertEquals(List.of("begin", "commit"), calls);
        }

        @Test
        void withTransaction_passesInputThrough() {
            var calls = new ArrayList<String>();
            var em = createEntityManager(createTransaction(calls));

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (Integer input) -> Promise.success(input * 2)
            );

            operation.apply(21)
                .await()
                .onFailure(cause -> fail("Should not fail: " + cause))
                .onSuccess(result -> assertEquals(42, result));

            assertEquals(List.of("begin", "commit"), calls);
        }
    }

    @Nested
    class TransactionBeginFailure {
        @Test
        void withTransaction_failsWhenBeginThrows() {
            var em = new EntityManagerStub() {
                @Override
                public EntityTransaction getTransaction() {
                    return new EntityTransactionStub() {
                        @Override
                        public void begin() {
                            throw new RuntimeException("Begin failed");
                        }

                        @Override
                        public boolean isActive() {
                            return false;
                        }
                    };
                }
            };

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (String _) -> Promise.success("result")
            );

            operation.apply("test")
                .await()
                .onSuccess(_ -> fail("Should fail"))
                .onFailure(cause -> {
                    assertInstanceOf(JpaError.DatabaseFailure.class, cause);
                    assertTrue(cause.message().contains("Begin failed"));
                });
        }
    }

    @Nested
    class OperationFailure {
        @Test
        void withTransaction_rollsBackOnOperationFailure() {
            var calls = new ArrayList<String>();
            var em = createEntityManager(createTransaction(calls, true));

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (String _) -> Promise.failure(JpaError.EntityNotFound.INSTANCE)
            );

            operation.apply("test")
                .await()
                .onSuccess(_ -> fail("Should fail"))
                .onFailure(cause -> assertInstanceOf(JpaError.EntityNotFound.class, cause));

            assertEquals(List.of("begin", "isActive", "rollback"), calls);
        }

        @Test
        void withTransaction_skipsRollbackWhenTransactionNotActive() {
            var calls = new ArrayList<String>();
            var em = createEntityManager(createTransaction(calls, false));

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (String _) -> Promise.failure(JpaError.EntityNotFound.INSTANCE)
            );

            operation.apply("test")
                .await()
                .onSuccess(_ -> fail("Should fail"));

            assertEquals(List.of("begin", "isActive"), calls);
        }

        @Test
        void withTransaction_logsButIgnoresRollbackFailure() {
            var calls = new ArrayList<String>();
            var em = new EntityManagerStub() {
                @Override
                public EntityTransaction getTransaction() {
                    return new EntityTransactionStub() {
                        @Override
                        public void begin() {
                            calls.add("begin");
                        }

                        @Override
                        public boolean isActive() {
                            calls.add("isActive");
                            return true;
                        }

                        @Override
                        public void rollback() {
                            calls.add("rollback");
                            throw new RuntimeException("Rollback failed");
                        }
                    };
                }
            };

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (String _) -> Promise.failure(JpaError.EntityNotFound.INSTANCE)
            );

            operation.apply("test")
                .await()
                .onSuccess(_ -> fail("Should fail"))
                 // Original failure preserved, not rollback exception
                .onFailure(cause -> assertInstanceOf(JpaError.EntityNotFound.class, cause));

            assertEquals(List.of("begin", "isActive", "rollback"), calls);
        }
    }

    @Nested
    class CommitFailure {
        @Test
        void withTransaction_rollsBackOnCommitFailure() {
            var calls = new ArrayList<String>();
            var em = new EntityManagerStub() {
                @Override
                public EntityTransaction getTransaction() {
                    return new EntityTransactionStub() {
                        @Override
                        public void begin() {
                            calls.add("begin");
                        }

                        @Override
                        public void commit() {
                            calls.add("commit");
                            throw new RuntimeException("Commit failed");
                        }

                        @Override
                        public boolean isActive() {
                            calls.add("isActive");
                            return true;
                        }

                        @Override
                        public void rollback() {
                            calls.add("rollback");
                        }
                    };
                }
            };

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (String _) -> Promise.success("result")
            );

            operation.apply("test")
                .await()
                .onSuccess(_ -> fail("Should fail"))
                .onFailure(cause -> {
                    assertInstanceOf(JpaError.DatabaseFailure.class, cause);
                    assertTrue(cause.message().contains("Commit failed"));
                });

            assertEquals(List.of("begin", "commit", "isActive", "rollback"), calls);
        }

        @Test
        void withTransaction_logsWhenRollbackAfterCommitFails() {
            var calls = new ArrayList<String>();
            var em = new EntityManagerStub() {
                @Override
                public EntityTransaction getTransaction() {
                    return new EntityTransactionStub() {
                        @Override
                        public void begin() {
                            calls.add("begin");
                        }

                        @Override
                        public void commit() {
                            calls.add("commit");
                            throw new RuntimeException("Commit failed");
                        }

                        @Override
                        public boolean isActive() {
                            calls.add("isActive");
                            return true;
                        }

                        @Override
                        public void rollback() {
                            calls.add("rollback");
                            throw new RuntimeException("Rollback failed");
                        }
                    };
                }
            };

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (String _) -> Promise.success("result")
            );

            operation.apply("test")
                .await()
                .onSuccess(_ -> fail("Should fail"))
                .onFailure(cause -> {
                    // Original commit failure preserved
                    assertInstanceOf(JpaError.DatabaseFailure.class, cause);
                    assertTrue(cause.message().contains("Commit failed"));
                });

            assertEquals(List.of("begin", "commit", "isActive", "rollback"), calls);
        }

        @Test
        void withTransaction_skipsRollbackWhenNotActiveAfterCommitFailure() {
            var calls = new ArrayList<String>();
            var em = new EntityManagerStub() {
                @Override
                public EntityTransaction getTransaction() {
                    return new EntityTransactionStub() {
                        @Override
                        public void begin() {
                            calls.add("begin");
                        }

                        @Override
                        public void commit() {
                            calls.add("commit");
                            throw new RuntimeException("Commit failed");
                        }

                        @Override
                        public boolean isActive() {
                            calls.add("isActive");
                            return false;
                        }
                    };
                }
            };

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (String _) -> Promise.success("result")
            );

            operation.apply("test")
                .await()
                .onSuccess(_ -> fail("Should fail"));

            assertEquals(List.of("begin", "commit", "isActive"), calls);
        }
    }

    @Nested
    class PromiseComposition {
        @Test
        void withTransaction_handlesPromiseChaining() {
            var calls = new ArrayList<String>();
            var em = createEntityManager(createTransaction(calls));

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (String input) -> Promise.success(input)
                    .map(s -> "async-" + s)
            );

            operation.apply("test")
                .await()
                .onFailure(cause -> fail("Should not fail: " + cause))
                .onSuccess(result -> assertEquals("async-test", result));

            assertEquals(List.of("begin", "commit"), calls);
        }

        @Test
        void withTransaction_handlesDelayedFailure() {
            var calls = new ArrayList<String>();
            var em = createEntityManager(createTransaction(calls, true));

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (String input) -> Promise.success(input)
                    .flatMap(_ -> Promise.failure(JpaError.EntityNotFound.INSTANCE))
            );

            operation.apply("test")
                .await()
                .onSuccess(_ -> fail("Should fail"))
                .onFailure(cause -> assertInstanceOf(JpaError.EntityNotFound.class, cause));

            assertEquals(List.of("begin", "isActive", "rollback"), calls);
        }
    }

    @Nested
    class SequentialOperations {
        @Test
        void withTransaction_handlesMultipleDecoratedOperations() {
            var calls = new ArrayList<String>();
            var em = createEntityManager(createTransaction(calls));

            var step1 = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (String input) -> Promise.success(input + "-step1")
            );

            var step2 = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (String input) -> Promise.success(input + "-step2")
            );

            step1.apply("test")
                .flatMap(step2)
                .await()
                .onFailure(cause -> fail("Should not fail: " + cause))
                .onSuccess(result -> assertEquals("test-step1-step2", result));

            // Each operation gets its own transaction
            assertEquals(List.of("begin", "commit", "begin", "commit"), calls);
        }
    }

    @Nested
    class UnitHandling {
        @Test
        void withTransaction_Fn0_worksWithUnitResult() {
            var calls = new ArrayList<String>();
            var em = createEntityManager(createTransaction(calls));

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                () -> Promise.success(Unit.unit())
            );

            operation.apply()
                .await()
                .onFailure(cause -> fail("Should not fail: " + cause))
                .onSuccess(result -> assertEquals(Unit.unit(), result));

            assertEquals(List.of("begin", "commit"), calls);
        }

        @Test
        void withTransaction_Fn1_acceptsUnitInput() {
            var calls = new ArrayList<String>();
            var em = createEntityManager(createTransaction(calls));

            var operation = Transactional.withTransaction(
                em,
                JpaError::fromException,
                (Unit _) -> Promise.success("result")
            );

            operation.apply(Unit.unit())
                .await()
                .onFailure(cause -> fail("Should not fail: " + cause))
                .onSuccess(result -> assertEquals("result", result));

            assertEquals(List.of("begin", "commit"), calls);
        }
    }

    @Nested
    class ErrorMapping {
        @Test
        void withTransaction_appliesCustomErrorMapper() {
            var em = new EntityManagerStub() {
                @Override
                public EntityTransaction getTransaction() {
                    return new EntityTransactionStub() {
                        @Override
                        public void begin() {
                            throw new RuntimeException("Custom error");
                        }

                        @Override
                        public boolean isActive() {
                            return false;
                        }
                    };
                }
            };

            var operation = Transactional.withTransaction(
                em,
                JpaError.DatabaseFailure::databaseFailure,
                (String _) -> Promise.success("result")
            );

            operation.apply("test")
                .await()
                .onSuccess(_ -> fail("Should fail"))
                .onFailure(cause -> {
                    assertInstanceOf(JpaError.DatabaseFailure.class, cause);
                    assertTrue(cause.message().contains("Custom error"));
                });
        }

        @Test
        void withTransaction_mapsSpecificException() {
            var em = new EntityManagerStub() {
                @Override
                public EntityTransaction getTransaction() {
                    return new EntityTransactionStub() {
                        @Override
                        public void begin() {
                            throw new IllegalStateException("State error");
                        }

                        @Override
                        public boolean isActive() {
                            return false;
                        }
                    };
                }
            };

            var operation = Transactional.withTransaction(
                em,
                t -> t instanceof IllegalStateException
                    ? JpaError.TransactionRequired.INSTANCE
                    : JpaError.DatabaseFailure.databaseFailure(t),
                (String _) -> Promise.success("result")
            );

            operation.apply("test")
                .await()
                .onSuccess(_ -> fail("Should fail"))
                .onFailure(cause -> assertInstanceOf(JpaError.TransactionRequired.class, cause));
        }
    }

    // Helper methods to create stubs

    private EntityManager createEntityManager(EntityTransaction tx) {
        return new EntityManagerStub() {
            @Override
            public EntityTransaction getTransaction() {
                return tx;
            }
        };
    }

    private EntityTransaction createTransaction(List<String> calls) {
        return createTransaction(calls, false);
    }

    private EntityTransaction createTransaction(List<String> calls, boolean isActive) {
        return new EntityTransactionStub() {
            @Override
            public void begin() {
                calls.add("begin");
            }

            @Override
            public void commit() {
                calls.add("commit");
            }

            @Override
            public void rollback() {
                calls.add("rollback");
            }

            @Override
            public boolean isActive() {
                calls.add("isActive");
                return isActive;
            }
        };
    }
}
