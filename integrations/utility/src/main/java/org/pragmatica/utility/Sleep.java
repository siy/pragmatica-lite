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

import org.pragmatica.lang.io.TimeSpan;

/// Simple sleep utility that handles interrupts gracefully.
public sealed interface Sleep {
    /// Sleep for the specified time span.
    /// If interrupted, restores the interrupt flag.
    static void sleep(TimeSpan span) {
        try{
            Thread.sleep(span.millis());
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
        }
    }

    @SuppressWarnings("unused")
    record unused() implements Sleep {}
}
