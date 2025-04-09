/*
 *  Copyright (c) 2023-2025 Sergiy Yevtushenko.
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

package org.pragmatica.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.io.CoreError;
import org.pragmatica.lang.utils.Causes;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.Unit.unit;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

public class PromiseTest {
    private static final Cause FAULT_CAUSE = new CoreError.Fault("Test fault");
    private static final Result<Integer> FAULT = FAULT_CAUSE.result();

    @Test
    void promiseCanBeResolved() {
        var promise = Promise.<Integer>promise();
        var ref = new AtomicInteger();

        promise.resolve(Result.success(1));
        promise.onSuccess(ref::set);

        promise.await().onSuccess(v -> assertEquals(1, v));

        assertEquals(1, ref.get());
    }

    @Test
    void promiseCanSucceedAsynchronously() {
        var ref = new AtomicInteger();

        Promise.<Integer>promise()
               .succeedAsync(() -> 1)
               .onSuccess(ref::set)
               .await()
               .onSuccess(v -> assertEquals(1, v));

        assertEquals(1, ref.get());
    }

    @Test
    void promiseCanFailAsynchronously() {
        Promise.<Integer>promise()
               .failAsync(() -> FAULT_CAUSE)
               .await()
               .onSuccessRun(Assertions::fail);
    }

    @Test
    void multipleAsyncResolutionsAreIgnored() throws InterruptedException {
        var successCounter = new AtomicInteger(0);
        var latch = new CountDownLatch(1);
        var promise = Promise.<Integer>promise().onSuccess(_ -> {
                                 if (successCounter.getAndIncrement() > 0) {
                                     fail("Promise must be resolved only once");
                                 }
                                 latch.countDown();
                             })
                             .onFailure(_ -> fail("Promise must be resolved successfully"));

        for (int i = 0; i < 1000; i++) {
            promise.succeedAsync(() -> 123);
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Success handler was not invoked in time");
    }

    @Test
    void resolvedPromiseCanBeCreated() {
        var promise = Promise.resolved(Result.success(1));

        assertTrue(promise.isResolved());
    }

    @Test
    void promiseCanBeCancelled() {
        var promise = Promise.<Integer>promise();

        promise.cancel().await()
               .onFailure(this::assertIsCancelled)
               .onSuccess(_ -> fail("Promise should be cancelled"));
    }

    @Test
    void successActionsAreExecutedAfterResolutionWithSuccess() throws InterruptedException {
        var latch = new CountDownLatch(1);
        var ref1 = new AtomicInteger();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise()
                             .onSuccess(ref1::set)
                             .onSuccessRun(() -> ref2.set(true))
                             .onSuccessRun(latch::countDown);

        assertEquals(0, ref1.get());
        assertFalse(ref2.get());

        promise.resolve(Result.success(1));

        if (!latch.await(1, TimeUnit.SECONDS)) {
            fail("Promise is not resolved");
        }

        assertEquals(1, ref1.get());
        assertTrue(ref2.get());
    }

    @Test
    void successActionsAreNotExecutedAfterResolutionWithFailure() {
        var ref1 = new AtomicInteger();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise()
                             .onSuccess(ref1::set)
                             .onSuccessRun(() -> ref2.set(true));

        assertEquals(0, ref1.get());
        assertFalse(ref2.get());

        promise.resolve(FAULT).await();

        assertEquals(0, ref1.get());
        assertFalse(ref2.get());
    }

    @Test
    void failureActionsAreExecutedAfterResolutionWithFailure() {
        var integerPromise = Promise.<Integer>promise();
        var booleanPromise = Promise.<Boolean>promise();

        var promise = Promise.<Integer>promise()
                             .onFailure(integerPromise::fail)
                             .onFailureRun(() -> booleanPromise.succeed(true));

        promise.resolve(FAULT);

        Promise.all(integerPromise, booleanPromise)
               .id()
               .await();

        integerPromise.await()
                      .onFailure(this::assertIsFault)
                      .onSuccessRun(Assertions::fail);
        booleanPromise.await()
                      .onSuccess(Assertions::assertTrue)
                      .onFailureRun(Assertions::fail);
    }

    @Test
    void failureActionsAreNotExecutedAfterResolutionWithSuccess() {
        var ref1 = new AtomicReference<Cause>();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise()
                             .onFailure(ref1::set)
                             .onFailureRun(() -> ref2.set(true));

        assertNull(ref1.get());
        assertFalse(ref2.get());

        promise.resolve(Result.success(1)).await();

        assertNull(ref1.get());
        assertFalse(ref2.get());
    }

    @Test
    void resultActionsAreExecutedAfterResolutionWithSuccess() {
        var integerPromise = Promise.<Integer>promise();
        var booleanPromise = Promise.<Boolean>promise();

        var promise = Promise.<Integer>promise()
                             .onResult(integerPromise::resolve)
                             .onResultRun(() -> booleanPromise.succeed(true));

        promise.resolve(Result.success(1));

        Promise.all(integerPromise, booleanPromise)
               .map((integer, bool) -> {
                   assertEquals(1, integer);
                   assertTrue(bool);
                   return unit();
               }).await();
    }

    @Test
    void resultActionsAreExecutedAfterResolutionWithFailure() {
        var integerPromise = Promise.<Integer>promise();
        var booleanPromise = Promise.<Boolean>promise();
        var promise = Promise.<Integer>promise()
                             .onResult(integerPromise::resolve)
                             .onResultRun(() -> booleanPromise.succeed(true));

        promise.resolve(FAULT).await();

        Promise.all(integerPromise, booleanPromise)
               .id()
               .await()
               .onFailure(this::assertIsFault)
               .onSuccessRun(Assertions::fail);

        assertTrue(booleanPromise.isResolved());

        booleanPromise.await()
                      .onSuccess(Assertions::assertTrue)
                      .onFailureRun(Assertions::fail);
    }

    @Test
    void promiseCanBeRecovered() {
        Promise.<Integer>err(Causes.cause("Test cause"))
               .onSuccess(_ -> fail("Promise should be failed"))
               .recover(_ -> 1)
               .await()
               .onSuccess(v -> assertEquals(1, v));
    }

    @Test
    void promiseResultCanBeMapped() {
        Promise.success(123)
               .onSuccess(v -> assertEquals(123, v))
               .mapResult(value -> Result.success(value + 1))
               .onSuccess(v -> assertEquals(124, v))
               .mapResult(_ -> Result.failure(Causes.cause("Test cause")))
               .await()
               .onSuccessRun(Assertions::fail);
    }

    @Test
    void promiseCanBeUsedAfterTimeout() {
        var promise = Promise.promise(timeSpan(100).millis(), p -> p.succeed(1));

        assertFalse(promise.isResolved());

        promise.await()
               .onSuccess(v -> assertEquals(1, v))
               .onFailureRun(Assertions::fail);
    }

    @Test
    void allPromisesCanBeFailedInBatch() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();

        assertFalse(promise1.isResolved());
        assertFalse(promise2.isResolved());
        assertFalse(promise3.isResolved());

        Promise.cancelAll(promise1, promise2, promise3);

        assertTrue(promise1.isResolved());
        assertTrue(promise2.isResolved());
        assertTrue(promise3.isResolved());

        assertTrue(promise1.await().isFailure());
        assertTrue(promise2.await().isFailure());
        assertTrue(promise3.await().isFailure());
    }

    @Test
    void awaitReturnsErrorAfterTimeoutThenPromiseRemainsInSameStateAndNoActionsAreExecuted() {
        var ref1 = new AtomicReference<Result<Integer>>();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise()
                             .onResult(ref1::set)
                             .onResultRun(() -> ref2.set(true));

        assertNull(ref1.get());
        assertFalse(ref2.get());

        promise.await(timeSpan(10).millis())
               .onFailure(this::assertIsTimeout)
               .onSuccess(_ -> fail("Timeout is expected"));

        assertNull(ref1.get());
        assertFalse(ref2.get());
    }

    @Test
    void resultActionsAreExecutedWhenPromiseIsResolvedToSuccess() throws InterruptedException {
        var latch = new CountDownLatch(3);
        var ref1 = new AtomicReference<Result<Integer>>();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise();

        var lastPromise = promise.onResult(newValue -> {
                                     ref1.set(newValue);
                                     latch.countDown();
                                 })
                                 .onResultRun(() -> {
                                     ref2.set(true);
                                     latch.countDown();
                                 })
                                 .withResult(_ -> latch.countDown());

        assertNull(ref1.get());
        assertFalse(ref2.get());

        promise.succeedAsync(() -> 1);

        latch.await();

        assertEquals(Result.success(1), ref1.get());
        assertTrue(ref2.get());
        assertTrue(lastPromise.isResolved());
    }

    @Test
    void resultActionsAreExecutedWhenPromiseIsResolvedToFailure() throws InterruptedException {
        var latch = new CountDownLatch(3);
        var ref1 = new AtomicReference<Result<Integer>>();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise();

        promise
                .onResult(newValue -> {
                    ref1.set(newValue);
                    latch.countDown();
                })
                .onResultRun(() -> {
                    ref2.set(true);
                    latch.countDown();
                })
                .withResult(_ -> latch.countDown());

        assertNull(ref1.get());
        assertFalse(ref2.get());

        promise.resolve(Result.failure(Causes.cause("Test cause")));

        latch.await();

        assertEquals(Result.failure(Causes.cause("Test cause")), ref1.get());
        assertTrue(ref2.get());
    }

    @Test
    void successActionsAreExecutedWhenPromiseIsResolvedToSuccess() throws InterruptedException {
        var latch = new CountDownLatch(3);
        var ref1 = new AtomicReference<Integer>();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise();

        promise.onSuccess(newValue -> {
                   ref1.set(newValue);
                   latch.countDown();
               })
               .onSuccessRun(() -> {
                   ref2.set(true);
                   latch.countDown();
               })
               .withSuccess(_ -> latch.countDown());

        assertNull(ref1.get());
        assertFalse(ref2.get());

        promise.succeedAsync(() -> 1);

        latch.await();

        assertEquals(1, ref1.get());
        assertTrue(ref2.get());
    }

    @Test
    void successActionsAreNotExecutedWhenPromiseIsResolvedToFailure() throws InterruptedException {
        var latch = new CountDownLatch(1);
        var ref1 = new AtomicReference<Integer>();
        var ref2 = new AtomicBoolean(false);
        var ref3 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise();

        promise.onSuccess(ref1::set)
               .onSuccessRun(() -> ref2.set(true))
               .withSuccess(_ -> ref3.set(true))
               .withFailure(_ -> latch.countDown());

        assertNull(ref1.get());
        assertFalse(ref2.get());
        assertFalse(ref3.get());

        promise.failAsync(() -> Causes.cause("Some cause"));

        latch.await();

        assertNull(ref1.get());
        assertFalse(ref2.get());
        assertFalse(ref3.get());
    }

    @Test
    void failureActionsAreExecutedWhenPromiseIsResolvedToFailure() throws InterruptedException {
        var latch = new CountDownLatch(3);
        var ref1 = new AtomicReference<Cause>();
        var ref2 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise();

        promise
                .onFailure(newValue -> {
                    ref1.set(newValue);
                    latch.countDown();
                })
                .onFailureRun(() -> {
                    ref2.set(true);
                    latch.countDown();
                })
                .withFailure(_ -> latch.countDown());

        assertNull(ref1.get());
        assertFalse(ref2.get());

        promise.resolve(Result.failure(Causes.cause("Test cause")));

        latch.await();

        assertEquals(Causes.cause("Test cause"), ref1.get());
        assertTrue(ref2.get());
    }

    @Test
    void failureActionsAreNotExecutedWhenPromiseIsResolvedToSuccess() throws InterruptedException {
        var latch = new CountDownLatch(1);
        var ref1 = new AtomicReference<Cause>();
        var ref2 = new AtomicBoolean(false);
        var ref3 = new AtomicBoolean(false);
        var promise = Promise.<Integer>promise();

        promise.onFailure(ref1::set)
               .onFailureRun(() -> ref2.set(true))
               .withFailure(_ -> ref3.set(true))
               .withSuccess(_ -> latch.countDown());

        assertNull(ref1.get());
        assertFalse(ref2.get());
        assertFalse(ref3.get());

        promise.succeedAsync(() -> 1);

        latch.await();

        assertNull(ref1.get());
        assertFalse(ref2.get());
        assertFalse(ref3.get());
    }

    @Test
    void alternativeCanBeChosenIfPromiseIsResolvedToFailure() {
        var promise = Promise.<Integer>promise();

        promise.failAsync(() -> Causes.cause("Test cause"))
               .orElse(Promise.success(1))
               .await()
               .onSuccess(v -> assertEquals(1, v))
               .onFailureRun(Assertions::fail);
    }

    @Test
    void alternativeCanBeChosenIfPromiseIsResolvedToFailure2() {
        var promise = Promise.<Integer>promise();

        promise.failAsync(() -> Causes.cause("Test cause"))
               .orElse(() -> Promise.ok(1))
               .await()
               .onSuccess(v -> assertEquals(1, v))
               .onFailureRun(Assertions::fail);
    }

    @Test
    void promiseCanBeMappedToUnit() {
        var promise = Promise.<Integer>promise();

        promise.succeedAsync(() -> 1)
               .mapToUnit()
               .await()
               .onSuccess(v -> assertEquals(Unit.unit(), v))
               .onFailureRun(Assertions::fail);
    }

    @Test
    void asyncActionIsExecutedAfterTimeout() {
        var ref1 = new AtomicLong(System.nanoTime());
        var ref2 = new AtomicLong();
        var ref3 = new AtomicLong();

        var promise = Promise.<Integer>promise()
                             .async(timeSpan(10).millis(), p -> {
                                 ref2.set(System.nanoTime());
                                 p.resolve(Result.success(1))
                                  .onResultRun(() -> ref3.set(System.nanoTime()));
                             });

        promise.await();

        //For informational purposes
        System.out.printf("From start of promise creation to start of async execution: %.2fms\n",
                          (ref2.get() - ref1.get()) / 1e6);
        System.out.printf("From start of async execution to start of execution of attached action: %.2fms\n",
                          (ref3.get() - ref2.get()) / 1e6);
        System.out.printf("Total execution time: %2fms\n", (ref3.get() - ref1.get()) / 1e6);

        // Expect that timeout should be between requested and twice as requested
        assertTrue((ref2.get() - ref1.get()) >= timeSpan(10).millis().nanos());
        assertTrue((ref2.get() - ref1.get()) < timeSpan(20).millis().nanos());
    }

    @Test
    void multipleActionsAreExecutedAfterResolution() {
        var promise = Promise.<Integer>promise();

        var integerPromise = Promise.promise();
        var stringPromise = Promise.<String>promise();
        var longPromise = Promise.<Long>promise();
        var counterPromise = Promise.<Integer>promise();
        var replacementPromise = Promise.<Long>promise();

        promise
                .onSuccess(integerPromise::succeed)
                .map(Objects::toString)
                .onSuccess(stringPromise::succeed)
                .map(Long::parseLong)
                .onSuccess(longPromise::succeed)
                .onSuccessRun(() -> {
                    try {
                        Thread.sleep(50);
                        counterPromise.succeed(1);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                })
                .map(() -> 123L)
                .onSuccess(replacementPromise::succeed);

        assertFalse(promise.isResolved());
        assertFalse(integerPromise.isResolved());
        assertFalse(stringPromise.isResolved());
        assertFalse(longPromise.isResolved());
        assertFalse(counterPromise.isResolved());

        promise.resolve(Result.success(1));

        Promise.all(integerPromise, stringPromise, longPromise, counterPromise, replacementPromise)
               .map((integer, string, aLong, counter, increment) -> {
                   assertEquals(1, integer);
                   assertEquals("1", string);
                   assertEquals(1L, aLong);
                   assertEquals(1, counter);
                   assertEquals(123L, increment);
                   return unit();
               })
               .flatMap(() -> Promise.failure(Causes.cause("Test cause")))
               .trace()
               .await()
               .onSuccessRun(Assertions::fail)
               .onFailure(System.out::println)
               .onFailure(cause -> assertInstanceOf(Causes.CompositeCause.class, cause));
    }

    @Test
    void all1ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();

        var allPromise = Promise.all(promise1).id();

        assertFalse(allPromise.isResolved());

        promise1.succeed(1);

        allPromise.await()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1), tuple))
                  .onFailureRun(Assertions::fail);
    }

    @Test
    void all2ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();

        var allPromise = Promise.all(promise1, promise2).id();

        assertFalse(allPromise.isResolved());

        promise1.succeed(1);
        promise2.succeed(2);

        allPromise.await()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2), tuple))
                  .onFailureRun(Assertions::fail);
    }

    @Test
    void all3ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();

        var allPromise = Promise.all(promise1, promise2, promise3).id();

        assertFalse(allPromise.isResolved());

        promise1.succeed(1);
        promise2.succeed(2);
        promise3.succeed(3);

        allPromise.await()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3), tuple))
                  .onFailureRun(Assertions::fail);
    }

    @Test
    void all4ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();
        var promise4 = Promise.<Integer>promise();

        var allPromise = Promise.all(
                promise1, promise2, promise3, promise4).id();

        assertFalse(allPromise.isResolved());

        promise1.succeed(1);
        promise2.succeed(2);
        promise3.succeed(3);
        promise4.succeed(4);

        allPromise.await()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3, 4), tuple))
                  .onFailureRun(Assertions::fail);
    }

    @Test
    void all5ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();
        var promise4 = Promise.<Integer>promise();
        var promise5 = Promise.<Integer>promise();

        var allPromise = Promise.all(
                promise1, promise2, promise3, promise4, promise5).id();

        assertFalse(allPromise.isResolved());

        promise1.succeed(1);
        promise2.succeed(2);
        promise3.succeed(3);
        promise4.succeed(4);
        promise5.succeed(5);

        allPromise.await()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3, 4, 5), tuple))
                  .onFailureRun(Assertions::fail);
    }

    @Test
    void all6ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();
        var promise4 = Promise.<Integer>promise();
        var promise5 = Promise.<Integer>promise();
        var promise6 = Promise.<Integer>promise();

        var allPromise = Promise.all(
                promise1, promise2, promise3, promise4, promise5, promise6).id();

        assertFalse(allPromise.isResolved());

        promise1.succeed(1);
        promise2.succeed(2);
        promise3.succeed(3);
        promise4.succeed(4);
        promise5.succeed(5);
        promise6.succeed(6);

        allPromise.await()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3, 4, 5, 6), tuple))
                  .onFailureRun(Assertions::fail);
    }

    @Test
    void all7ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();
        var promise4 = Promise.<Integer>promise();
        var promise5 = Promise.<Integer>promise();
        var promise6 = Promise.<Integer>promise();
        var promise7 = Promise.<Integer>promise();

        var allPromise = Promise.all(
                promise1, promise2, promise3, promise4, promise5, promise6, promise7).id();

        assertFalse(allPromise.isResolved());

        promise1.succeed(1);
        promise2.succeed(2);
        promise3.succeed(3);
        promise4.succeed(4);
        promise5.succeed(5);
        promise6.succeed(6);
        promise7.succeed(7);

        allPromise.await()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3, 4, 5, 6, 7), tuple))
                  .onFailureRun(Assertions::fail);
    }

    @Test
    void all8ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();
        var promise4 = Promise.<Integer>promise();
        var promise5 = Promise.<Integer>promise();
        var promise6 = Promise.<Integer>promise();
        var promise7 = Promise.<Integer>promise();
        var promise8 = Promise.<Integer>promise();

        var allPromise = Promise.all(
                promise1, promise2, promise3, promise4, promise5,
                promise6, promise7, promise8).id();

        assertFalse(allPromise.isResolved());

        promise1.succeed(1);
        promise2.succeed(2);
        promise3.succeed(3);
        promise4.succeed(4);
        promise5.succeed(5);
        promise6.succeed(6);
        promise7.succeed(7);
        promise8.succeed(8);

        allPromise.await()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3, 4, 5, 6, 7, 8), tuple))
                  .onFailureRun(Assertions::fail);
    }

    @Test
    void all9ResolvedToSuccessIfAllParametersResolvedToSuccess() {
        var promise1 = Promise.<Integer>promise();
        var promise2 = Promise.<Integer>promise();
        var promise3 = Promise.<Integer>promise();
        var promise4 = Promise.<Integer>promise();
        var promise5 = Promise.<Integer>promise();
        var promise6 = Promise.<Integer>promise();
        var promise7 = Promise.<Integer>promise();
        var promise8 = Promise.<Integer>promise();
        var promise9 = Promise.<Integer>promise();

        var allPromise = Promise.all(
                promise1, promise2, promise3, promise4, promise5,
                promise6, promise7, promise8, promise9).id();

        assertFalse(allPromise.isResolved());

        promise1.succeed(1);
        promise2.succeed(2);
        promise3.succeed(3);
        promise4.succeed(4);
        promise5.succeed(5);
        promise6.succeed(6);
        promise7.succeed(7);
        promise8.succeed(8);
        promise9.succeed(9);

        allPromise.await()
                  .onSuccess(tuple -> assertEquals(Tuple.tuple(1, 2, 3, 4, 5, 6, 7, 8, 9), tuple))
                  .onFailureRun(Assertions::fail);
    }

    @Test
    void promiseCanBeConfiguredAsynchronously() throws InterruptedException {
        var ref = new AtomicInteger(0);
        var latch = new CountDownLatch(1);

        var promise = Promise.<Integer>promise(p -> p.onSuccess(ref::set).onSuccessRun(latch::countDown));

        promise.succeed(1);
        latch.await();

        assertEquals(1, ref.get());
    }

    void assertIsFault(Cause cause) {
        switch (cause) {
            case CoreError.Fault _ -> {
            }
            case Causes.CompositeCause compositeCause -> {
                if (compositeCause.isEmpty()) {
                    fail("Composite cause is empty");
                }
                compositeCause.stream().forEach(this::assertIsFault);
            }
            default -> fail("Unexpected cause");
        }
    }

    void assertIsTimeout(Cause cause) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (cause) {
            case CoreError.Timeout _ -> {
            }
            default -> fail("Unexpected cause");
        }
    }

    void assertIsCancelled(Cause cause) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (cause) {
            case CoreError.Cancelled _ -> {
            }
            default -> fail("Unexpected cause");
        }
    }
}