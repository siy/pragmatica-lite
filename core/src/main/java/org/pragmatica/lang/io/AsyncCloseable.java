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

package org.pragmatica.lang.io;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

/**
 * Interface for resources that can be closed asynchronously.
 * <p>
 * Unlike {@link java.io.Closeable}, this interface returns a {@link Promise}
 * that completes when the resource has been fully released.
 */
public interface AsyncCloseable {
    /**
     * Asynchronously close this resource.
     *
     * @return a promise that completes when the resource is closed
     */
    Promise<Unit> close();
}
