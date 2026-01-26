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

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/// Seedable random source for reproducible property-based testing.
public interface RandomSource {
    /// Generate a random integer.
    int nextInt();

    /// Generate a random integer in range [0, bound).
    int nextInt(int bound);

    /// Generate a random integer in range [min, max].
    int nextInt(int min, int max);

    /// Generate a random long.
    long nextLong();

    /// Generate a random long in range [min, max].
    long nextLong(long min, long max);

    /// Generate a random double in range [0.0, 1.0).
    double nextDouble();

    /// Generate a random boolean.
    boolean nextBoolean();

    /// Create a seeded random source for reproducible tests.
    static RandomSource seeded(long seed) {
        return new SeededRandomSource(seed);
    }

    /// Create a random source using ThreadLocalRandom.
    static RandomSource random() {
        return new ThreadLocalRandomSource();
    }
}

/// Thread-safe seeded random source. All methods are synchronized to ensure
/// reproducible results even when accessed from multiple threads.
final class SeededRandomSource implements RandomSource {
    private final Random random;

    SeededRandomSource(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public synchronized int nextInt() {
        return random.nextInt();
    }

    @Override
    public synchronized int nextInt(int bound) {
        return random.nextInt(bound);
    }

    @Override
    public synchronized int nextInt(int min, int max) {
        return random.nextInt(min, max + 1);
    }

    @Override
    public synchronized long nextLong() {
        return random.nextLong();
    }

    @Override
    public synchronized long nextLong(long min, long max) {
        return random.nextLong(min, max + 1);
    }

    @Override
    public synchronized double nextDouble() {
        return random.nextDouble();
    }

    @Override
    public synchronized boolean nextBoolean() {
        return random.nextBoolean();
    }
}

final class ThreadLocalRandomSource implements RandomSource {
    @Override
    public int nextInt() {
        return ThreadLocalRandom.current()
                                .nextInt();
    }

    @Override
    public int nextInt(int bound) {
        return ThreadLocalRandom.current()
                                .nextInt(bound);
    }

    @Override
    public int nextInt(int min, int max) {
        return ThreadLocalRandom.current()
                                .nextInt(min, max + 1);
    }

    @Override
    public long nextLong() {
        return ThreadLocalRandom.current()
                                .nextLong();
    }

    @Override
    public long nextLong(long min, long max) {
        return ThreadLocalRandom.current()
                                .nextLong(min, max + 1);
    }

    @Override
    public double nextDouble() {
        return ThreadLocalRandom.current()
                                .nextDouble();
    }

    @Override
    public boolean nextBoolean() {
        return ThreadLocalRandom.current()
                                .nextBoolean();
    }
}
