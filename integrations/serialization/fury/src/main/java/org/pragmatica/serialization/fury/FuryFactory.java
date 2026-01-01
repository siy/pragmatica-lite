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

package org.pragmatica.serialization.fury;

import org.pragmatica.serialization.ClassRegistrator;

import java.util.stream.Stream;

import org.apache.fury.Fury;
import org.apache.fury.ThreadSafeFury;
import org.apache.fury.config.Language;

/**
 * Factory for creating thread-safe Fury instances.
 */
public sealed interface FuryFactory {
    /**
     * Create a thread-safe Fury instance with the given class registrators.
     *
     * @param registrators class registrators to apply
     * @return a thread-safe Fury instance
     */
    static ThreadSafeFury fury(ClassRegistrator... registrators) {
        int coreCount = Runtime.getRuntime()
                               .availableProcessors();
        var fury = Fury.builder()
                       .withLanguage(Language.JAVA)
                       .buildThreadSafeFuryPool(coreCount * 2, coreCount * 4);
        Stream.of(registrators)
              .forEach(registrator -> registrator.registerClasses(fury::register));
        return fury;
    }

    @SuppressWarnings("unused")
    record unused() implements FuryFactory {}
}
