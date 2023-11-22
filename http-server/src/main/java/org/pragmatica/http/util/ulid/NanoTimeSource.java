/*
 * Copyright (c) 2020 Sergiy Yevtushenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pragmatica.http.util.ulid;

import java.time.Instant;

/**
 * Time source with nanosecond resolution.
 * <p>
 * WARNING: this time source is derived from {@link System#nanoTime()} and is not accurate!
 */
public final class NanoTimeSource {
    private static final long NANOS_PER_MILLI = 1_000_000;
    private static final long NANOS_PER_SECOND = 1_000_000_000;
    private static final long OFFSET = System.currentTimeMillis() * NANOS_PER_MILLI - System.nanoTime();

    private NanoTimeSource() {
    }

    public static long timestamp() {
        return OFFSET + System.nanoTime();
    }

    public static Instant instant() {
        long stamp = timestamp();

        return Instant.ofEpochSecond(stamp / NANOS_PER_SECOND,
                                     stamp % NANOS_PER_SECOND);
    }
}
