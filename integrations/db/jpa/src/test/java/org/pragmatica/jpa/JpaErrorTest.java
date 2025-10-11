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

import jakarta.persistence.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JpaErrorTest {

    @Nested
    class ExceptionMapping {
        @Test
        void fromException_mapsEntityNotFoundException() {
            var error = JpaError.fromException(new EntityNotFoundException("Entity not found"));

            assertInstanceOf(JpaError.EntityNotFound.class, error);
            assertEquals("Entity not found", error.message());
        }

        @Test
        void fromException_mapsNoResultException() {
            var error = JpaError.fromException(new NoResultException("No result"));

            assertInstanceOf(JpaError.EntityNotFound.class, error);
        }

        @Test
        void fromException_mapsOptimisticLockException() {
            var entity = new TestEntity();
            var exception = new OptimisticLockException(entity);
            var error = JpaError.fromException(exception);

            assertInstanceOf(JpaError.OptimisticLock.class, error);
            JpaError.OptimisticLock lockError = (JpaError.OptimisticLock) error;
            assertEquals("TestEntity", lockError.entityType());
        }

        @Test
        void fromException_mapsPessimisticLockException() {
            var error = JpaError.fromException(new PessimisticLockException("Lock failed"));

            assertInstanceOf(JpaError.PessimisticLock.class, error);
            assertTrue(error.message().contains("Lock failed"));
        }

        @Test
        void fromException_mapsLockTimeoutException() {
            var error = JpaError.fromException(new LockTimeoutException("Timeout"));

            assertInstanceOf(JpaError.PessimisticLock.class, error);
            assertTrue(error.message().contains("Lock timeout"));
        }

        @Test
        void fromException_mapsEntityExistsException() {
            var error = JpaError.fromException(new EntityExistsException("Already exists"));

            assertInstanceOf(JpaError.EntityExists.class, error);
        }

        @Test
        void fromException_mapsQueryTimeoutException() {
            var error = JpaError.fromException(new QueryTimeoutException("Query timeout"));

            assertInstanceOf(JpaError.QueryTimeout.class, error);
        }

        @Test
        void fromException_mapsTransactionRequiredException() {
            var error = JpaError.fromException(new TransactionRequiredException("No transaction"));

            assertInstanceOf(JpaError.TransactionRequired.class, error);
            assertEquals("Transaction is required for this operation", error.message());
        }

        @Test
        void fromException_mapsUnknownException() {
            var error = JpaError.fromException(new RuntimeException("Unknown error"));

            assertInstanceOf(JpaError.DatabaseFailure.class, error);
            assertTrue(error.message().contains("Unknown error"));
        }
    }

    static class TestEntity {
        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }
}
