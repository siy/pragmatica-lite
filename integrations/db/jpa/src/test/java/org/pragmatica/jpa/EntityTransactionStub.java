/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
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

package org.pragmatica.jpa;

import jakarta.persistence.EntityTransaction;

/// Stub implementation of EntityTransaction for testing.
///
/// See [EntityManagerStub] for explanation why stubs are used instead of Mockito mocks.
public interface EntityTransactionStub extends EntityTransaction {

    @Override
    default void begin() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void commit() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void rollback() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void setRollbackOnly() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default boolean getRollbackOnly() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default boolean isActive() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default Integer getTimeout() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void setTimeout(Integer timeout) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }
}
