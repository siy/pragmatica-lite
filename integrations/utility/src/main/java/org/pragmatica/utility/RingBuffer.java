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

package org.pragmatica.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

/**
 * Thread-safe ring buffer with fixed capacity.
 *
 * <p>Provides O(1) add operations and fixed memory footprint.
 * When the buffer is full, oldest elements are overwritten.
 *
 * @param <T> element type
 */
public final class RingBuffer<T> {
    private final Object[] buffer;
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private int head;

    // Next write position
    private int size;

    // Current number of elements
    private RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new Object[capacity];
        this.head = 0;
        this.size = 0;
    }

    /**
     * Create a new ring buffer with the specified capacity.
     */
    public static <T> RingBuffer<T> ringBuffer(int capacity) {
        return new RingBuffer<>(capacity);
    }

    /**
     * Add an element to the buffer. If full, overwrites the oldest element.
     */
    public void add(T element) {
        lock.lock();
        try{
            buffer[head] = element;
            head = (head + 1) % capacity;
            if (size < capacity) {
                size++ ;
            }
        } finally{
            lock.unlock();
        }
    }

    /**
     * Get all elements in order from oldest to newest.
     */
    public List<T> toList() {
        lock.lock();
        try{
            var result = new ArrayList<T>(size);
            if (size == 0) {
                return result;
            }
            // Start from the oldest element
            int start = (size < capacity)
                        ? 0
                        : head;
            for (int i = 0; i < size; i++ ) {
                int idx = (start + i) % capacity;
                @SuppressWarnings("unchecked")
                T element = (T) buffer[idx];
                result.add(element);
            }
            return result;
        } finally{
            lock.unlock();
        }
    }

    /**
     * Get all elements that match the predicate, in order from oldest to newest.
     */
    public List<T> filter(Predicate<T> predicate) {
        lock.lock();
        try{
            var result = new ArrayList<T>();
            if (size == 0) {
                return result;
            }
            // Start from the oldest element
            int start = (size < capacity)
                        ? 0
                        : head;
            for (int i = 0; i < size; i++ ) {
                int idx = (start + i) % capacity;
                @SuppressWarnings("unchecked")
                T element = (T) buffer[idx];
                if (predicate.test(element)) {
                    result.add(element);
                }
            }
            return result;
        } finally{
            lock.unlock();
        }
    }

    /**
     * Get the current number of elements.
     */
    public int size() {
        lock.lock();
        try{
            return size;
        } finally{
            lock.unlock();
        }
    }

    /**
     * Check if the buffer is empty.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Get the maximum capacity.
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Clear all elements.
     */
    public void clear() {
        lock.lock();
        try{
            for (int i = 0; i < capacity; i++ ) {
                buffer[i] = null;
            }
            head = 0;
            size = 0;
        } finally{
            lock.unlock();
        }
    }
}
