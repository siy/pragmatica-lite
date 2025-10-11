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

package org.pragmatica.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.lang.Result.failure;
import static org.pragmatica.lang.Result.success;

class PromiseMetricsTest {
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
    }

    @Test
    void timerMetrics_recordsSuccessfulOperation() {
        var metrics = PromiseMetrics.timer("test.operation")
                                    .registry(registry)
                                    .tags("type", "test")
                                    .build();

        var operation = metrics.around(() -> Promise.success("result"));

        operation.get().await();

        var successTimer = registry.find("test.operation")
                                  .tag("result", "success")
                                  .tag("type", "test")
                                  .timer();

        assertThat(successTimer).isNotNull();
        assertThat(successTimer.count()).isEqualTo(1);
    }

    @Test
    void timerMetrics_recordsFailedOperation() {
        var metrics = PromiseMetrics.timer("test.operation")
                                    .registry(registry)
                                    .tags("type", "test")
                                    .build();

        var operation = metrics.around(() -> TestCause.INSTANCE.promise());

        operation.get().await();

        var failureTimer = registry.find("test.operation")
                                  .tag("result", "failure")
                                  .tag("type", "test")
                                  .timer();

        assertThat(failureTimer).isNotNull();
        assertThat(failureTimer.count()).isEqualTo(1);
    }

    @Test
    void timerMetrics_recordsExecutionTime() throws InterruptedException {
        var metrics = PromiseMetrics.timer("test.operation")
                                    .registry(registry)
                                    .build();

        var operation = metrics.around(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Promise.success("result");
        });

        operation.get().await();

        var timer = registry.find("test.operation")
                           .tag("result", "success")
                           .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(100);
    }

    @Test
    void counterMetrics_recordsSuccessfulOperation() {
        var metrics = PromiseMetrics.counter("test.operation")
                                    .registry(registry)
                                    .tags("type", "test")
                                    .build();

        var operation = metrics.around(() -> Promise.success("result"));

        operation.get().await();

        var successCounter = registry.find("test.operation.success")
                                    .tag("type", "test")
                                    .counter();

        assertThat(successCounter).isNotNull();
        assertThat(successCounter.count()).isEqualTo(1);
    }

    @Test
    void counterMetrics_recordsFailedOperation() {
        var metrics = PromiseMetrics.counter("test.operation")
                                    .registry(registry)
                                    .tags("type", "test")
                                    .build();

        var operation = metrics.around(() -> TestCause.INSTANCE.promise());

        operation.get().await();

        var failureCounter = registry.find("test.operation.failure")
                                    .tag("type", "test")
                                    .counter();

        assertThat(failureCounter).isNotNull();
        assertThat(failureCounter.count()).isEqualTo(1);
    }

    @Test
    void counterMetrics_recordsMultipleOperations() {
        var metrics = PromiseMetrics.counter("test.operation")
                                    .registry(registry)
                                    .build();

        var operation = metrics.around(() -> Promise.success("result"));

        operation.get().await();
        operation.get().await();
        operation.get().await();

        var successCounter = registry.find("test.operation.success").counter();

        assertThat(successCounter).isNotNull();
        assertThat(successCounter.count()).isEqualTo(3);
    }

    @Test
    void combinedMetrics_recordsBothTimerAndCounters() {
        var metrics = PromiseMetrics.combined("test.operation")
                                    .registry(registry)
                                    .tags("type", "test")
                                    .build();

        var operation = metrics.around(() -> Promise.success("result"));

        operation.get().await();

        var timer = registry.find("test.operation")
                           .tag("type", "test")
                           .timer();
        var successCounter = registry.find("test.operation.success")
                                    .tag("type", "test")
                                    .counter();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(successCounter).isNotNull();
        assertThat(successCounter.count()).isEqualTo(1);
    }

    @Test
    void timerMetrics_worksWithFunction() {
        var metrics = PromiseMetrics.timer("test.operation")
                                    .registry(registry)
                                    .build();

        var function = metrics.around((String input) -> Promise.success(input.toUpperCase()));

        function.apply("test").await();

        var timer = registry.find("test.operation")
                           .tag("result", "success")
                           .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void counterMetrics_worksWithFunction() {
        var metrics = PromiseMetrics.counter("test.operation")
                                    .registry(registry)
                                    .build();

        var function = metrics.around((String input) -> Promise.success(input.toUpperCase()));

        function.apply("test").await();

        var counter = registry.find("test.operation.success").counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void timerMetrics_separatesSuccessAndFailureCounts() {
        var metrics = PromiseMetrics.timer("test.operation")
                                    .registry(registry)
                                    .build();

        var successOp = metrics.around(() -> Promise.success("ok"));
        var failureOp = metrics.around(() -> TestCause.INSTANCE.promise());

        successOp.get().await();
        successOp.get().await();
        failureOp.get().await();

        var successTimer = registry.find("test.operation")
                                  .tag("result", "success")
                                  .timer();
        var failureTimer = registry.find("test.operation")
                                  .tag("result", "failure")
                                  .timer();

        assertThat(successTimer.count()).isEqualTo(2);
        assertThat(failureTimer.count()).isEqualTo(1);
    }

    private enum TestCause implements org.pragmatica.lang.Cause {
        INSTANCE;

        @Override
        public String message() {
            return "Test failure";
        }
    }
}
