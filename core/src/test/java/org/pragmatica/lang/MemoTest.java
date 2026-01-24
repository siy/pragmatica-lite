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

package org.pragmatica.lang;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class MemoTest {

    @Nested
    class UnboundedMemo {

        @Test
        void get_cachesComputedValues() {
            var counter = new AtomicInteger(0);
            var memo = Memo.memo((Integer key) -> {
                counter.incrementAndGet();
                return key * 2;
            });

            assertEquals(10, memo.get(5));
            assertEquals(10, memo.get(5));
            assertEquals(10, memo.get(5));

            assertEquals(1, counter.get());
        }

        @Test
        void get_computesForDifferentKeys() {
            var counter = new AtomicInteger(0);
            var memo = Memo.memo((Integer key) -> {
                counter.incrementAndGet();
                return key * 2;
            });

            assertEquals(10, memo.get(5));
            assertEquals(20, memo.get(10));
            assertEquals(30, memo.get(15));

            assertEquals(3, counter.get());
        }

        @Test
        void hitCount_tracksHits() {
            var memo = Memo.memo((Integer key) -> key * 2);

            memo.get(5);
            assertEquals(0, memo.hitCount());
            assertEquals(1, memo.missCount());

            memo.get(5);
            assertEquals(1, memo.hitCount());
            assertEquals(1, memo.missCount());

            memo.get(5);
            assertEquals(2, memo.hitCount());
            assertEquals(1, memo.missCount());
        }

        @Test
        void missCount_tracksMisses() {
            var memo = Memo.memo((Integer key) -> key * 2);

            memo.get(1);
            memo.get(2);
            memo.get(3);

            assertEquals(0, memo.hitCount());
            assertEquals(3, memo.missCount());
        }

        @Test
        void size_returnsNumberOfCachedEntries() {
            var memo = Memo.memo((Integer key) -> key * 2);

            assertEquals(0, memo.size());

            memo.get(1);
            assertEquals(1, memo.size());

            memo.get(2);
            assertEquals(2, memo.size());

            memo.get(1);
            assertEquals(2, memo.size());
        }

        @Test
        void invalidate_removesEntry() {
            var counter = new AtomicInteger(0);
            var memo = Memo.memo((Integer key) -> {
                counter.incrementAndGet();
                return key * 2;
            });

            memo.get(5);
            assertEquals(1, counter.get());
            assertEquals(1, memo.size());

            memo.invalidate(5);
            assertEquals(0, memo.size());

            memo.get(5);
            assertEquals(2, counter.get());
        }

        @Test
        void invalidateAll_removesAllEntries() {
            var memo = Memo.memo((Integer key) -> key * 2);

            memo.get(1);
            memo.get(2);
            memo.get(3);
            assertEquals(3, memo.size());

            memo.invalidateAll();
            assertEquals(0, memo.size());
        }

        @Test
        void get_threadSafeUnderConcurrentAccess() throws InterruptedException {
            var threadCount = 100;
            var counter = new AtomicInteger(0);
            var memo = Memo.memo((Integer key) -> {
                counter.incrementAndGet();
                return key * 2;
            });

            var latch = new CountDownLatch(threadCount);
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        try {
                            assertEquals(10, memo.get(5));
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await();
            }

            assertEquals(1, counter.get());
        }

        @Test
        void get_threadSafeWithMultipleKeys() throws InterruptedException {
            var threadCount = 100;
            var counter = new AtomicInteger(0);
            var memo = Memo.memo((Integer key) -> {
                counter.incrementAndGet();
                return key * 2;
            });

            var latch = new CountDownLatch(threadCount);
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    int key = i % 10;
                    executor.submit(() -> {
                        try {
                            assertEquals(key * 2, memo.get(key));
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await();
            }

            assertEquals(10, counter.get());
        }
    }

    @Nested
    class LruEviction {

        @Test
        void memo_throwsForNonPositiveMaxSize() {
            assertThrows(IllegalArgumentException.class, () -> Memo.memo(k -> k, 0));
            assertThrows(IllegalArgumentException.class, () -> Memo.memo(k -> k, -1));
        }

        @Test
        void get_evictsLeastRecentlyUsed() {
            var memo = Memo.memo((Integer key) -> key * 2, 3);

            memo.get(1);
            memo.get(2);
            memo.get(3);
            assertEquals(3, memo.size());

            memo.get(4);
            assertEquals(3, memo.size());
        }

        @Test
        void get_updatesAccessOrderOnHit() {
            var counter = new AtomicInteger(0);
            var memo = Memo.memo((Integer key) -> {
                counter.incrementAndGet();
                return key * 2;
            }, 3);

            // Add 1, 2, 3
            memo.get(1);
            memo.get(2);
            memo.get(3);

            // Touch 1 to make it recently used
            memo.get(1);

            // Add 4 - should evict 2 (oldest after 1 was touched)
            memo.get(4);

            // Key 1 should still be cached
            assertEquals(2, memo.get(1));
            assertEquals(4, counter.get()); // 1,2,3,4 computed

            // Key 2 should have been evicted and needs recomputation
            assertEquals(4, memo.get(2));
            assertEquals(5, counter.get()); // 2 recomputed
        }

        @Test
        void hitCount_tracksHits() {
            var memo = Memo.memo((Integer key) -> key * 2, 10);

            memo.get(5);
            assertEquals(0, memo.hitCount());

            memo.get(5);
            assertEquals(1, memo.hitCount());

            memo.get(5);
            assertEquals(2, memo.hitCount());
        }

        @Test
        void missCount_tracksMisses() {
            var memo = Memo.memo((Integer key) -> key * 2, 10);

            memo.get(1);
            memo.get(2);
            memo.get(3);

            assertEquals(0, memo.hitCount());
            assertEquals(3, memo.missCount());
        }

        @Test
        void invalidate_removesEntry() {
            var memo = Memo.memo((Integer key) -> key * 2, 10);

            memo.get(5);
            assertEquals(1, memo.size());

            memo.invalidate(5);
            assertEquals(0, memo.size());
        }

        @Test
        void invalidateAll_removesAllEntries() {
            var memo = Memo.memo((Integer key) -> key * 2, 10);

            memo.get(1);
            memo.get(2);
            memo.get(3);
            assertEquals(3, memo.size());

            memo.invalidateAll();
            assertEquals(0, memo.size());
        }

        @Test
        void get_threadSafeUnderConcurrentAccess() throws InterruptedException {
            var threadCount = 100;
            var counter = new AtomicInteger(0);
            var memo = Memo.memo((Integer key) -> {
                counter.incrementAndGet();
                return key * 2;
            }, 50);

            var latch = new CountDownLatch(threadCount);
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        try {
                            assertEquals(10, memo.get(5));
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await();
            }

            assertEquals(1, counter.get());
        }

        @Test
        void get_threadSafeWithEviction() throws InterruptedException {
            var threadCount = 100;
            var memo = Memo.memo((Integer key) -> key * 2, 10);

            var latch = new CountDownLatch(threadCount);
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    int key = i;
                    executor.submit(() -> {
                        try {
                            assertEquals(key * 2, memo.get(key));
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await();
            }

            assertTrue(memo.size() <= 10);
            assertEquals(100, memo.missCount());
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void invalidate_nonexistentKeyIsNoOp() {
            var memo = Memo.memo((Integer key) -> key * 2);

            memo.invalidate(999);
            assertEquals(0, memo.size());
        }

        @Test
        void invalidateAll_onEmptyCacheIsNoOp() {
            var memo = Memo.memo((Integer key) -> key * 2);

            memo.invalidateAll();
            assertEquals(0, memo.size());
        }
    }
}
