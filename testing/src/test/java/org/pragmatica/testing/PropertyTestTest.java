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

package org.pragmatica.testing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;

import static org.junit.jupiter.api.Assertions.*;

class PropertyTestTest {

    private static void failOnCause(Cause cause) {
        fail("Unexpected failure: " + cause.message());
    }

    @Nested
    class PassingProperties {
        @Test
        void check_passes_forAlwaysTrueProperty() {
            PropertyTest.forAll(Arbitraries.integers())
                .tries(100)
                .seed(42)
                .check(_ -> true)
                .onFailure(PropertyTestTest::failOnCause)
                .onSuccess(result -> {
                    assertInstanceOf(PropertyResult.Passed.class, result);
                    assertEquals(100, ((PropertyResult.Passed) result).tries());
                });
        }

        @Test
        void check_passes_forValidProperty() {
            PropertyTest.forAll(Arbitraries.integers(1, 100))
                .tries(100)
                .seed(42)
                .check(n -> n >= 1 && n <= 100)
                .onFailure(PropertyTestTest::failOnCause)
                .onSuccess(result -> assertInstanceOf(PropertyResult.Passed.class, result));
        }

        @Test
        void checkResult_passes_forSuccessfulProperty() {
            PropertyTest.forAll(Arbitraries.integers())
                .tries(50)
                .seed(42)
                .checkResult(_ -> Result.unitResult())
                .onFailure(PropertyTestTest::failOnCause)
                .onSuccess(result -> {
                    assertInstanceOf(PropertyResult.Passed.class, result);
                    assertEquals(50, ((PropertyResult.Passed) result).tries());
                });
        }
    }

    @Nested
    class FailingProperties {
        @Test
        void check_fails_forAlwaysFalseProperty() {
            PropertyTest.forAll(Arbitraries.integers())
                .tries(100)
                .seed(42)
                .check(_ -> false)
                .onFailure(PropertyTestTest::failOnCause)
                .onSuccess(result -> {
                    assertInstanceOf(PropertyResult.Failed.class, result);
                    assertEquals(1, ((PropertyResult.Failed) result).tryNumber());
                });
        }

        @Test
        void check_fails_withCorrectFailureInfo() {
            PropertyTest.forAll(Arbitraries.integers(10, 100))
                .tries(100)
                .seed(42)
                .check(n -> n < 50)
                .onFailure(PropertyTestTest::failOnCause)
                .onSuccess(result -> {
                    assertInstanceOf(PropertyResult.Failed.class, result);
                    PropertyResult.Failed failed = (PropertyResult.Failed) result;
                    assertTrue(failed.tryNumber() >= 1);
                    assertNotNull(failed.originalInput());
                    assertNotNull(failed.shrunkInput());
                });
        }

        @Test
        void checkResult_fails_forFailingResult() {
            PropertyTest.forAll(Arbitraries.integers())
                .tries(100)
                .seed(42)
                .checkResult(_ -> TestError.INSTANCE.result())
                .onFailure(PropertyTestTest::failOnCause)
                .onSuccess(result -> assertInstanceOf(PropertyResult.Failed.class, result));
        }
    }

    @Nested
    class Shrinking {
        @Test
        void shrinking_findsMinimalCase() {
            PropertyTest.forAll(Arbitraries.integers(0, 1000))
                .tries(100)
                .seed(42)
                .shrinkingDepth(100)
                .check(n -> n <= 50)
                .onFailure(PropertyTestTest::failOnCause)
                .onSuccess(result -> {
                    assertInstanceOf(PropertyResult.Failed.class, result);
                    PropertyResult.Failed failed = (PropertyResult.Failed) result;
                    int shrunk = (Integer) failed.shrunkInput();
                    assertEquals(51, shrunk, "Should shrink to boundary value 51");
                });
        }

        @Test
        void shrinking_recordsSteps() {
            PropertyTest.forAll(Arbitraries.integers(0, 1000))
                .tries(100)
                .seed(42)
                .check(n -> n < 500)
                .onFailure(PropertyTestTest::failOnCause)
                .onSuccess(result -> {
                    assertInstanceOf(PropertyResult.Failed.class, result);
                    PropertyResult.Failed failed = (PropertyResult.Failed) result;
                    assertTrue(failed.shrinkSteps() > 0, "Should have shrinking steps");
                });
        }
    }

    @Nested
    class Reproducibility {
        @Test
        void sameSeed_producesIdenticalResults() {
            var result1 = PropertyTest.forAll(Arbitraries.integers())
                .tries(100)
                .seed(12345)
                .check(n -> n > 0);

            var result2 = PropertyTest.forAll(Arbitraries.integers())
                .tries(100)
                .seed(12345)
                .check(n -> n > 0);

            result1.onFailure(PropertyTestTest::failOnCause)
                   .onSuccess(r1 -> result2.onFailure(PropertyTestTest::failOnCause)
                                           .onSuccess(r2 -> {
                                               if (r1 instanceof PropertyResult.Failed f1 &&
                                                   r2 instanceof PropertyResult.Failed f2) {
                                                   assertEquals(f1.tryNumber(), f2.tryNumber());
                                                   assertEquals(f1.originalInput(), f2.originalInput());
                                               } else if (r1 instanceof PropertyResult.Passed &&
                                                          r2 instanceof PropertyResult.Passed) {
                                                   // Both passed - fine
                                               } else {
                                                   fail("Results should be identical");
                                               }
                                           }));
        }
    }

    @Nested
    class MultipleArbitraries {
        @Test
        void forAll2_testsTwoValues() {
            PropertyTest.forAll(Arbitraries.integers(1, 100), Arbitraries.integers(1, 100))
                .tries(100)
                .seed(42)
                .check((a, b) -> a + b >= 2)
                .onFailure(PropertyTestTest::failOnCause)
                .onSuccess(result -> assertInstanceOf(PropertyResult.Passed.class, result));
        }

        @Test
        void forAll3_testsThreeValues() {
            PropertyTest.forAll(
                Arbitraries.integers(1, 10),
                Arbitraries.integers(1, 10),
                Arbitraries.integers(1, 10)
            )
                .tries(100)
                .seed(42)
                .check((a, b, c) -> a + b + c >= 3)
                .onFailure(PropertyTestTest::failOnCause)
                .onSuccess(result -> assertInstanceOf(PropertyResult.Passed.class, result));
        }

        @Test
        void forAll2_fails_correctly() {
            PropertyTest.forAll(Arbitraries.integers(0, 100), Arbitraries.integers(0, 100))
                .tries(100)
                .seed(42)
                .check((a, b) -> a + b < 50)
                .onFailure(PropertyTestTest::failOnCause)
                .onSuccess(result -> assertInstanceOf(PropertyResult.Failed.class, result));
        }
    }

    @Nested
    class TriesConfiguration {
        @Test
        void customTries_runsSpecifiedNumber() {
            PropertyTest.forAll(Arbitraries.integers())
                .tries(25)
                .seed(42)
                .check(_ -> true)
                .onFailure(PropertyTestTest::failOnCause)
                .onSuccess(result -> {
                    assertInstanceOf(PropertyResult.Passed.class, result);
                    assertEquals(25, ((PropertyResult.Passed) result).tries());
                });
        }
    }

    enum TestError implements org.pragmatica.lang.Cause {
        INSTANCE;

        @Override
        public String message() {
            return "Test error";
        }
    }
}
