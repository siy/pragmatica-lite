package org.pragmatica.lang.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Functions.Fn2;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.utils.CircuitBreaker.CircuitBreakerErrors.CircuitBreakerOpenError;
import org.pragmatica.lang.utils.CircuitBreaker.State;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

class CircuitBreakerTest {
    private static final Cause TEST_ERROR = () -> "Test error";
    private static final Cause IGNORED_ERROR = () -> "Ignored error";

    private CircuitBreaker circuitBreaker;
    private TestTimeSource timeSource;

    private static class TestTimeSource implements TimeSource {
        private TimeSpan currentTime = timeSpan(0).nanos();

        @Override
        public long nanoTime() {
            return currentTime.nanos();
        }

        public void advanceTime(long millis) {
            currentTime = currentTime.plus(millis, TimeUnit.MILLISECONDS);
        }
    }

    @BeforeEach
    void setUp() {
        timeSource = new TestTimeSource();

        circuitBreaker = CircuitBreaker.builder()
                                       .failureThreshold(3)
                                       .resetTimeout(timeSpan(100).millis())
                                       .testAttempts(2)
                                       .shouldTrip(cause -> cause == TEST_ERROR)
                                       .timeSource(timeSource);
    }

    @Test
    void shouldExecuteSuccessfulOperation() {
        circuitBreaker.execute(() -> Promise.success("Success"))
                      .await()
                      .onFailureRun(Assertions::fail)
                      .onSuccess(value -> assertEquals("Success", value));

        assertEquals(State.CLOSED, circuitBreaker.state());
        assertEquals(0, circuitBreaker.failureCount());
    }

    @Test
    void shouldRecordFailures() {
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();

        assertEquals(State.CLOSED, circuitBreaker.state());
        assertEquals(2, circuitBreaker.failureCount());
    }

    @Test
    void shouldIgnoreNonTrippingFailures() {
        circuitBreaker.execute(IGNORED_ERROR::promise).await();
        circuitBreaker.execute(IGNORED_ERROR::promise).await();
        circuitBreaker.execute(IGNORED_ERROR::promise).await();
        circuitBreaker.execute(IGNORED_ERROR::promise).await();

        assertEquals(State.CLOSED, circuitBreaker.state());
        assertEquals(0, circuitBreaker.failureCount());
    }

    @Test
    void shouldOpenCircuitAfterThresholdExceeded() {
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();

        assertEquals(State.OPEN, circuitBreaker.state());
    }

    @Test
    void shouldRejectOperationsWhenOpen() {
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();

        var callCount = new AtomicInteger(0);
        circuitBreaker.execute(() -> {
                          callCount.incrementAndGet();
                          return Promise.success("Should not be executed");
                      })
                      .await()
                      .onSuccessRun(Assertions::fail)
                      .onFailure(cause -> assertInstanceOf(CircuitBreakerOpenError.class,
                                                           cause));
        assertEquals(0, callCount.get(), "Operation should not be executed when circuit is open");
    }

    @Test
    void shouldTransitionToHalfOpenAfterTimeout() {
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();
        assertEquals(State.OPEN, circuitBreaker.state());

        timeSource.advanceTime(150); // longer than reset timeout

        circuitBreaker.execute(() -> Promise.success("Test operation"))
                      .await()
                      .onFailureRun(Assertions::fail);

        assertEquals(State.HALF_OPEN, circuitBreaker.state());
    }

    @Test
    void shouldTransitionToClosedAfterSuccessfulTests() {
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();

        assertEquals(State.OPEN, circuitBreaker.state());

        // Move to HALF_OPEN state by advancing time
        timeSource.advanceTime(150);

        circuitBreaker.execute(() -> Promise.success("Test 1")).await();
        var result = circuitBreaker.execute(() -> Promise.success("Test 2")).await();

        assertTrue(result.isSuccess());
        assertEquals(State.CLOSED, circuitBreaker.state());
        assertEquals(0, circuitBreaker.failureCount());
    }

    @Test
    void shouldTransitionBackToOpenOnFailureDuringHalfOpen() {
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();
        assertEquals(State.OPEN, circuitBreaker.state());

        // Move to HALF_OPEN state by advancing time
        timeSource.advanceTime(150);

        // Verify we're in HALF_OPEN state
        circuitBreaker.execute(() -> Promise.success("First operation")).await();
        assertEquals(State.HALF_OPEN, circuitBreaker.state());

        circuitBreaker.execute(TEST_ERROR::<String>promise)
                      .await()
                      .onSuccessRun(Assertions::fail);

        assertEquals(State.OPEN, circuitBreaker.state());
    }

    @Test
    void shouldProvideCorrectTimeSinceLastStateChange() {
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();

        timeSource.advanceTime(50);

        assertEquals(50, circuitBreaker.timeSinceLastStateChange().millis());
    }

    @Test
    void shouldProvideCorrectRetryTimeInOpenError() {
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();

        timeSource.advanceTime(40);

        circuitBreaker.execute(() -> Promise.success("Should not execute"))
                      .await()
                      .onSuccessRun(Assertions::fail)
                      .onFailure(cause -> {
                          if (cause instanceof CircuitBreakerOpenError error) {
                              assertEquals(60, error.retryTime().millis());
                          } else {
                              Assertions.fail("Unexpected cause type: " + cause.getClass().getName());
                          }
                      });
    }

    @Test
    void shouldRequireMultipleSuccessfulTestsToClose() {
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();
        circuitBreaker.execute(TEST_ERROR::promise).await();

        timeSource.advanceTime(150);

        circuitBreaker.execute(() -> Promise.success("First test")).await();

        assertEquals(State.HALF_OPEN, circuitBreaker.state());

        circuitBreaker.execute(() -> Promise.success("Second test"))
                      .await()
                      .onFailureRun(Assertions::fail);

        assertEquals(State.CLOSED, circuitBreaker.state());
    }

    @Test
    void shouldHandleConcurrentExecutions() {
        final int threads = 10;

        // Create a special circuit breaker with real time source for this test
var breaker = CircuitBreaker.builder()
                            .failureThreshold(3)
                            .resetTimeout(timeSpan(100).millis())
                            .testAttempts(2)
                            .shouldTrip(cause -> cause == TEST_ERROR)
                            .withDefaultTimeSource();

        Fn2<Promise<String>, Integer, Promise<String>> halfFail = (index, promise) ->
                (index < 5)
                        ? promise.fail(TEST_ERROR)
                        : promise.succeed("Success from thread " + index);

        var promises = IntStream.range(0, threads)
                                .mapToObj(index -> breaker.execute(() -> Promise.<String>promise(
                                        promise -> halfFail.apply(index, promise))))
                                .toList();

        Promise.allOf(promises)
               .await(timeSpan(2).seconds())
               .onFailureRun(Assertions::fail)
               .onSuccess(values -> assertEquals(threads, values.size()));
    }

    @Test
    void shouldAllowMultipleOperationsWhenClosed() {
        for (int i = 0; i < 10; i++) {
            final var index = i;

            circuitBreaker.execute(() -> Promise.success(index))
                          .await()
                          .onFailureRun(Assertions::fail)
                          .onSuccess(value -> assertEquals(index, value));
        }

        assertEquals(State.CLOSED, circuitBreaker.state());
    }

    @Test
    void shouldCountOnlyConfiguredFailures() {
        circuitBreaker.execute(TEST_ERROR::promise).await();   // Counts toward threshold
        circuitBreaker.execute(IGNORED_ERROR::promise).await(); // Doesn't count
        circuitBreaker.execute(TEST_ERROR::promise).await();   // Counts toward threshold

        assertEquals(State.CLOSED, circuitBreaker.state());
        assertEquals(2, circuitBreaker.failureCount());

        circuitBreaker.execute(TEST_ERROR::promise).await();

        assertEquals(State.OPEN, circuitBreaker.state());
    }
}