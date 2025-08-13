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

package org.pragmatica.lang.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;

import java.util.concurrent.TimeUnit;

import static org.pragmatica.lang.Result.success;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
public class ResultVsPromiseBenchmark {
    
    // Test data
    private Result<String> successResult;
    private Promise<String> resolvedPromise;
    private Result<Integer> intResult;
    private Promise<Integer> intPromise;
    private Result<LargeObject> largeResult;
    private Promise<LargeObject> largePromise;
    
    @Setup
    public void setup() {
        successResult = success("test-string");
        resolvedPromise = Promise.resolved(success("test-string"));
        intResult = success(42);
        intPromise = Promise.resolved(success(42));
        
        LargeObject largeObj = new LargeObject();
        largeResult = success(largeObj);
        largePromise = Promise.resolved(success(largeObj));
    }

    // ===================================================================================
    // Simple Map Operations - String Values
    // ===================================================================================

    @Benchmark
    public void resultMapString(Blackhole bh) {
        var result = successResult.map(s -> s.toUpperCase());
        bh.consume(result);
    }

    @Benchmark
    public void promiseMapResolvedString(Blackhole bh) {
        var result = resolvedPromise.map(s -> s.toUpperCase());
        bh.consume(result);
    }

    // ===================================================================================
    // Simple Map Operations - Integer Values  
    // ===================================================================================

    @Benchmark
    public void resultMapInt(Blackhole bh) {
        var result = intResult.map(i -> i * 2);
        bh.consume(result);
    }

    @Benchmark
    public void promiseMapResolvedInt(Blackhole bh) {
        var result = intPromise.map(i -> i * 2);
        bh.consume(result);
    }

    // ===================================================================================
    // Simple Map Operations - Large Objects
    // ===================================================================================

    @Benchmark
    public void resultMapLarge(Blackhole bh) {
        var result = largeResult.map(obj -> obj.processedData());
        bh.consume(result);
    }

    @Benchmark
    public void promiseMapResolvedLarge(Blackhole bh) {
        var result = largePromise.map(obj -> obj.processedData());
        bh.consume(result);
    }

    // ===================================================================================
    // Simple FlatMap Operations
    // ===================================================================================

    @Benchmark
    public void resultFlatMapString(Blackhole bh) {
        var result = successResult.flatMap(s -> success(s.length()));
        bh.consume(result);
    }

    @Benchmark
    public void promiseFlatMapResolvedString(Blackhole bh) {
        var result = resolvedPromise.flatMap(s -> Promise.resolved(success(s.length())));
        bh.consume(result);
    }

    @Benchmark
    public void resultFlatMapInt(Blackhole bh) {
        var result = intResult.flatMap(i -> success(Integer.toString(i)));
        bh.consume(result);
    }

    @Benchmark
    public void promiseFlatMapResolvedInt(Blackhole bh) {
        var result = intPromise.flatMap(i -> Promise.resolved(success(Integer.toString(i))));
        bh.consume(result);
    }

    // ===================================================================================
    // Chain Operations - Short Chains (5 operations)
    // ===================================================================================

    @Benchmark
    public void resultMapChain5(Blackhole bh) {
        var result = intResult
                .map(i -> i + 1)
                .map(i -> i * 2)
                .map(i -> i - 5)
                .map(i -> i / 2)
                .map(i -> Integer.toString(i));
        bh.consume(result);
    }

    @Benchmark
    public void promiseMapChain5(Blackhole bh) {
        var result = intPromise
                .map(i -> i + 1)
                .map(i -> i * 2)
                .map(i -> i - 5)
                .map(i -> i / 2)
                .map(i -> Integer.toString(i));
        bh.consume(result);
    }

    @Benchmark
    public void resultFlatMapChain5(Blackhole bh) {
        var result = intResult
                .flatMap(i -> success(i + 1))
                .flatMap(i -> success(i * 2))
                .flatMap(i -> success(i - 5))
                .flatMap(i -> success(i / 2))
                .flatMap(i -> success(Integer.toString(i)));
        bh.consume(result);
    }

    @Benchmark
    public void promiseFlatMapChain5(Blackhole bh) {
        var result = intPromise
                .flatMap(i -> Promise.resolved(success(i + 1)))
                .flatMap(i -> Promise.resolved(success(i * 2)))
                .flatMap(i -> Promise.resolved(success(i - 5)))
                .flatMap(i -> Promise.resolved(success(i / 2)))
                .flatMap(i -> Promise.resolved(success(Integer.toString(i))));
        bh.consume(result);
    }

    // ===================================================================================
    // Chain Operations - Medium Chains (10 operations)
    // ===================================================================================

    @Benchmark
    public void resultMapChain10(Blackhole bh) {
        var result = intResult
                .map(i -> i + 1).map(i -> i * 2).map(i -> i - 1).map(i -> i / 3).map(i -> i + 5)
                .map(i -> i * 4).map(i -> i - 2).map(i -> i / 2).map(i -> i + 10).map(i -> Integer.toString(i));
        bh.consume(result);
    }

    @Benchmark
    public void promiseMapChain10(Blackhole bh) {
        var result = intPromise
                .map(i -> i + 1).map(i -> i * 2).map(i -> i - 1).map(i -> i / 3).map(i -> i + 5)
                .map(i -> i * 4).map(i -> i - 2).map(i -> i / 2).map(i -> i + 10).map(i -> Integer.toString(i));
        bh.consume(result);
    }

    @Benchmark
    public void resultFlatMapChain10(Blackhole bh) {
        var result = intResult
                .flatMap(i -> success(i + 1)).flatMap(i -> success(i * 2)).flatMap(i -> success(i - 1))
                .flatMap(i -> success(i / 3)).flatMap(i -> success(i + 5)).flatMap(i -> success(i * 4))
                .flatMap(i -> success(i - 2)).flatMap(i -> success(i / 2)).flatMap(i -> success(i + 10))
                .flatMap(i -> success(Integer.toString(i)));
        bh.consume(result);
    }

    @Benchmark
    public void promiseFlatMapChain10(Blackhole bh) {
        var result = intPromise
                .flatMap(i -> Promise.resolved(success(i + 1))).flatMap(i -> Promise.resolved(success(i * 2)))
                .flatMap(i -> Promise.resolved(success(i - 1))).flatMap(i -> Promise.resolved(success(i / 3)))
                .flatMap(i -> Promise.resolved(success(i + 5))).flatMap(i -> Promise.resolved(success(i * 4)))
                .flatMap(i -> Promise.resolved(success(i - 2))).flatMap(i -> Promise.resolved(success(i / 2)))
                .flatMap(i -> Promise.resolved(success(i + 10))).flatMap(i -> Promise.resolved(success(Integer.toString(i))));
        bh.consume(result);
    }

    // ===================================================================================
    // Chain Operations - Long Chains (20 operations)
    // ===================================================================================

    @Benchmark
    public void resultMapChain20(Blackhole bh) {
        var result = intResult
                .map(i -> i + 1).map(i -> i * 2).map(i -> i - 1).map(i -> i + 3).map(i -> i / 2)
                .map(i -> i * 3).map(i -> i - 2).map(i -> i + 5).map(i -> i / 4).map(i -> i * 5)
                .map(i -> i - 3).map(i -> i + 7).map(i -> i / 3).map(i -> i * 2).map(i -> i - 4)
                .map(i -> i + 6).map(i -> i / 5).map(i -> i * 7).map(i -> i - 1).map(i -> Integer.toString(i));
        bh.consume(result);
    }

    @Benchmark
    public void promiseMapChain20(Blackhole bh) {
        var result = intPromise
                .map(i -> i + 1).map(i -> i * 2).map(i -> i - 1).map(i -> i + 3).map(i -> i / 2)
                .map(i -> i * 3).map(i -> i - 2).map(i -> i + 5).map(i -> i / 4).map(i -> i * 5)
                .map(i -> i - 3).map(i -> i + 7).map(i -> i / 3).map(i -> i * 2).map(i -> i - 4)
                .map(i -> i + 6).map(i -> i / 5).map(i -> i * 7).map(i -> i - 1).map(i -> Integer.toString(i));
        bh.consume(result);
    }

    // ===================================================================================
    // Mixed Operations - Map + FlatMap combinations
    // ===================================================================================

    @Benchmark
    public void resultMixedChain(Blackhole bh) {
        var result = intResult
                .map(i -> i + 1)
                .flatMap(i -> success(i * 2))
                .map(i -> i - 5)
                .flatMap(i -> success(i / 2))
                .map(i -> i + 10)
                .flatMap(i -> success(Integer.toString(i)));
        bh.consume(result);
    }

    @Benchmark
    public void promiseMixedChain(Blackhole bh) {
        var result = intPromise
                .map(i -> i + 1)
                .flatMap(i -> Promise.resolved(success(i * 2)))
                .map(i -> i - 5)
                .flatMap(i -> Promise.resolved(success(i / 2)))
                .map(i -> i + 10)
                .flatMap(i -> Promise.resolved(success(Integer.toString(i))));
        bh.consume(result);
    }

    // ===================================================================================
    // Memory Allocation Tests - Creating new objects
    // ===================================================================================

    @Benchmark
    public void resultObjectCreation(Blackhole bh) {
        var result = successResult.map(s -> new SmallObject(s, s.length()));
        bh.consume(result);
    }

    @Benchmark
    public void promiseObjectCreation(Blackhole bh) {
        var result = resolvedPromise.map(s -> new SmallObject(s, s.length()));
        bh.consume(result);
    }

    @Benchmark
    public void resultLargeObjectCreation(Blackhole bh) {
        var result = successResult.map(s -> new LargeObject(s));
        bh.consume(result);
    }

    @Benchmark
    public void promiseLargeObjectCreation(Blackhole bh) {
        var result = resolvedPromise.map(s -> new LargeObject(s));
        bh.consume(result);
    }

    // ===================================================================================
    // Helper Classes for Testing
    // ===================================================================================

    record SmallObject(String data, int size) {}

    static class LargeObject {
        private final String data;
        private final byte[] largeArray;
        private final long timestamp;
        private final double[] calculations;

        public LargeObject() {
            this("default");
        }

        public LargeObject(String data) {
            this.data = data;
            this.largeArray = new byte[1024]; // 1KB array
            this.timestamp = System.currentTimeMillis();
            this.calculations = new double[100];
            
            // Fill arrays with some data
            for (int i = 0; i < largeArray.length; i++) {
                largeArray[i] = (byte) (i % 256);
            }
            for (int i = 0; i < calculations.length; i++) {
                calculations[i] = Math.sqrt(i);
            }
        }

        public String processedData() {
            return data + "_processed_" + timestamp;
        }

        public int getTotalSize() {
            return data.length() + largeArray.length + calculations.length * 8;
        }
    }
}