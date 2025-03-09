/*
 *  Copyright (c) 2023-2025 Sergiy Yevtushenko.
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

package org.pragmatica.lang.io;

import org.pragmatica.lang.Cause;

public sealed interface CoreError extends Cause {
    record Cancelled(String message) implements CoreError {}
    record Timeout(String message) implements CoreError {}
    record Fault(String message) implements CoreError {}
    record Exception(String message, Throwable cause) implements CoreError {
        public Exception(Throwable cause) {
            this(cause.getMessage(), cause);
        }
    }

    enum CoreErrors implements CoreError {
        EMPTY_OPTION("The instance of Option is empty");

        private final String message;

        CoreErrors(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    static CoreError emptyOption() {
        return CoreErrors.EMPTY_OPTION;
    }

    static CoreError exception(Throwable cause) {
        return new Exception(cause);
    }
}
