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

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;

import static org.pragmatica.lang.Result.success;

/// Simple performance analysis tool for Result vs Promise operations
/// Provides basic timing measurements for hot path optimization decisions
public class PerformanceAnalysis {

    private static final int WARMUP_ITERATIONS = 10_000;
    private static final int MEASUREMENT_ITERATIONS = 1_000_000;
    
    public static void main(String[] args) {
        System.out.println("=== Result vs Promise Performance Analysis ===");
        System.out.println("Warmup iterations: " + WARMUP_ITERATIONS);
        System.out.println("Measurement iterations: " + MEASUREMENT_ITERATIONS);
        System.out.println();
        
        analyzeMapOperations();
        analyzeFlatMapOperations();
        analyzeMapChains();
        analyzeFlatMapChains();
        analyzeMixedChains();
        
        System.out.println("\n=== Analysis Complete ===");
    }
    
    private static void analyzeMapOperations() {
        System.out.println("--- Simple Map Operations ---");
        
        var successResult = success(42);
        var resolvedPromise = Promise.resolved(success(42));
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            successResult.map(x -> x * 2);
            resolvedPromise.map(x -> x * 2);
        }
        
        // Measure Result.map
        long startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            successResult.map(x -> x * 2);
        }
        long resultMapTime = System.nanoTime() - startTime;
        
        // Measure Promise.map (resolved)
        startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            resolvedPromise.map(x -> x * 2);
        }
        long promiseMapTime = System.nanoTime() - startTime;
        
        System.out.printf("Result.map:   %,d ns total, %.2f ns/op\n", 
                         resultMapTime, (double) resultMapTime / MEASUREMENT_ITERATIONS);
        System.out.printf("Promise.map:  %,d ns total, %.2f ns/op\n", 
                         promiseMapTime, (double) promiseMapTime / MEASUREMENT_ITERATIONS);
        System.out.printf("Performance ratio (Promise/Result): %.2fx\n", 
                         (double) promiseMapTime / resultMapTime);
        System.out.println();
    }
    
    private static void analyzeFlatMapOperations() {
        System.out.println("--- Simple FlatMap Operations ---");
        
        var successResult = success(42);
        var resolvedPromise = Promise.resolved(success(42));
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            successResult.flatMap(x -> success(x * 2));
            resolvedPromise.flatMap(x -> Promise.resolved(success(x * 2)));
        }
        
        // Measure Result.flatMap
        long startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            successResult.flatMap(x -> success(x * 2));
        }
        long resultFlatMapTime = System.nanoTime() - startTime;
        
        // Measure Promise.flatMap (resolved)
        startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            resolvedPromise.flatMap(x -> Promise.resolved(success(x * 2)));
        }
        long promiseFlatMapTime = System.nanoTime() - startTime;
        
        System.out.printf("Result.flatMap:   %,d ns total, %.2f ns/op\n", 
                         resultFlatMapTime, (double) resultFlatMapTime / MEASUREMENT_ITERATIONS);
        System.out.printf("Promise.flatMap:  %,d ns total, %.2f ns/op\n", 
                         promiseFlatMapTime, (double) promiseFlatMapTime / MEASUREMENT_ITERATIONS);
        System.out.printf("Performance ratio (Promise/Result): %.2fx\n", 
                         (double) promiseFlatMapTime / resultFlatMapTime);
        System.out.println();
    }
    
    private static void analyzeMapChains() {
        System.out.println("--- Map Chain Operations (5 steps) ---");
        
        var successResult = success(42);
        var resolvedPromise = Promise.resolved(success(42));
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            successResult
                .map(x -> x + 1)
                .map(x -> x * 2)
                .map(x -> x - 5)
                .map(x -> x / 2)
                .map(x -> x + 10);
            resolvedPromise
                .map(x -> x + 1)
                .map(x -> x * 2)
                .map(x -> x - 5)
                .map(x -> x / 2)
                .map(x -> x + 10);
        }
        
        // Measure Result map chain
        long startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            successResult
                .map(x -> x + 1)
                .map(x -> x * 2)
                .map(x -> x - 5)
                .map(x -> x / 2)
                .map(x -> x + 10);
        }
        long resultChainTime = System.nanoTime() - startTime;
        
        // Measure Promise map chain
        startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            resolvedPromise
                .map(x -> x + 1)
                .map(x -> x * 2)
                .map(x -> x - 5)
                .map(x -> x / 2)
                .map(x -> x + 10);
        }
        long promiseChainTime = System.nanoTime() - startTime;
        
        System.out.printf("Result map chain:   %,d ns total, %.2f ns/op\n", 
                         resultChainTime, (double) resultChainTime / MEASUREMENT_ITERATIONS);
        System.out.printf("Promise map chain:  %,d ns total, %.2f ns/op\n", 
                         promiseChainTime, (double) promiseChainTime / MEASUREMENT_ITERATIONS);
        System.out.printf("Performance ratio (Promise/Result): %.2fx\n", 
                         (double) promiseChainTime / resultChainTime);
        System.out.println();
    }
    
    private static void analyzeFlatMapChains() {
        System.out.println("--- FlatMap Chain Operations (5 steps) ---");
        
        var successResult = success(42);
        var resolvedPromise = Promise.resolved(success(42));
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            successResult
                .flatMap(x -> success(x + 1))
                .flatMap(x -> success(x * 2))
                .flatMap(x -> success(x - 5))
                .flatMap(x -> success(x / 2))
                .flatMap(x -> success(x + 10));
            resolvedPromise
                .flatMap(x -> Promise.resolved(success(x + 1)))
                .flatMap(x -> Promise.resolved(success(x * 2)))
                .flatMap(x -> Promise.resolved(success(x - 5)))
                .flatMap(x -> Promise.resolved(success(x / 2)))
                .flatMap(x -> Promise.resolved(success(x + 10)));
        }
        
        // Measure Result flatMap chain
        long startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            successResult
                .flatMap(x -> success(x + 1))
                .flatMap(x -> success(x * 2))
                .flatMap(x -> success(x - 5))
                .flatMap(x -> success(x / 2))
                .flatMap(x -> success(x + 10));
        }
        long resultChainTime = System.nanoTime() - startTime;
        
        // Measure Promise flatMap chain
        startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            resolvedPromise
                .flatMap(x -> Promise.resolved(success(x + 1)))
                .flatMap(x -> Promise.resolved(success(x * 2)))
                .flatMap(x -> Promise.resolved(success(x - 5)))
                .flatMap(x -> Promise.resolved(success(x / 2)))
                .flatMap(x -> Promise.resolved(success(x + 10)));
        }
        long promiseChainTime = System.nanoTime() - startTime;
        
        System.out.printf("Result flatMap chain:   %,d ns total, %.2f ns/op\n", 
                         resultChainTime, (double) resultChainTime / MEASUREMENT_ITERATIONS);
        System.out.printf("Promise flatMap chain:  %,d ns total, %.2f ns/op\n", 
                         promiseChainTime, (double) promiseChainTime / MEASUREMENT_ITERATIONS);
        System.out.printf("Performance ratio (Promise/Result): %.2fx\n", 
                         (double) promiseChainTime / resultChainTime);
        System.out.println();
    }
    
    private static void analyzeMixedChains() {
        System.out.println("--- Mixed Map/FlatMap Chain Operations ---");
        
        var successResult = success(42);
        var resolvedPromise = Promise.resolved(success(42));
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            successResult
                .map(x -> x + 1)
                .flatMap(x -> success(x * 2))
                .map(x -> x - 5)
                .flatMap(x -> success(x / 2))
                .map(x -> x + 10);
            resolvedPromise
                .map(x -> x + 1)
                .flatMap(x -> Promise.resolved(success(x * 2)))
                .map(x -> x - 5)
                .flatMap(x -> Promise.resolved(success(x / 2)))
                .map(x -> x + 10);
        }
        
        // Measure Result mixed chain
        long startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            successResult
                .map(x -> x + 1)
                .flatMap(x -> success(x * 2))
                .map(x -> x - 5)
                .flatMap(x -> success(x / 2))
                .map(x -> x + 10);
        }
        long resultChainTime = System.nanoTime() - startTime;
        
        // Measure Promise mixed chain
        startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            resolvedPromise
                .map(x -> x + 1)
                .flatMap(x -> Promise.resolved(success(x * 2)))
                .map(x -> x - 5)
                .flatMap(x -> Promise.resolved(success(x / 2)))
                .map(x -> x + 10);
        }
        long promiseChainTime = System.nanoTime() - startTime;
        
        System.out.printf("Result mixed chain:   %,d ns total, %.2f ns/op\n", 
                         resultChainTime, (double) resultChainTime / MEASUREMENT_ITERATIONS);
        System.out.printf("Promise mixed chain:  %,d ns total, %.2f ns/op\n", 
                         promiseChainTime, (double) promiseChainTime / MEASUREMENT_ITERATIONS);
        System.out.printf("Performance ratio (Promise/Result): %.2fx\n", 
                         (double) promiseChainTime / resultChainTime);
        System.out.println();
    }
}