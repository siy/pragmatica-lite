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
import org.pragmatica.lang.Cause;

/// Typed error causes for JPA operations.
/// Maps common JPA exceptions to domain-friendly error types.
public sealed interface JpaError extends Cause {

    /// Entity not found by ID or query returned no results.
    enum EntityNotFound implements JpaError {
        INSTANCE;

        @Override
        public String message() {
            return "Entity not found";
        }
    }

    /// Optimistic locking conflict - entity was modified by another transaction.
    record OptimisticLock(String entityType, Object id) implements JpaError {
        @Override
        public String message() {
            return "Optimistic lock failure for " + entityType + " with id=" + id;
        }
    }

    /// Pessimistic locking timeout or failure.
    record PessimisticLock(String details) implements JpaError {
        @Override
        public String message() {
            return "Pessimistic lock failure: " + details;
        }
    }

    /// Database constraint violation (unique, foreign key, etc).
    record ConstraintViolation(String constraint) implements JpaError {
        @Override
        public String message() {
            return "Database constraint violated: " + constraint;
        }
    }

    /// Transaction required but not active.
    enum TransactionRequired implements JpaError {
        INSTANCE;

        @Override
        public String message() {
            return "Transaction is required for this operation";
        }
    }

    /// Entity already exists (duplicate insert).
    record EntityExists(String entityType, Object id) implements JpaError {
        @Override
        public String message() {
            return "Entity already exists: " + entityType + " with id=" + id;
        }
    }

    /// Query timeout exceeded.
    record QueryTimeout(String query) implements JpaError {
        @Override
        public String message() {
            return "Query timeout: " + query;
        }
    }

    /// General database failure (catch-all for unexpected errors).
    record DatabaseFailure(Throwable cause) implements JpaError {
        public static DatabaseFailure cause(Throwable e) {
            return new DatabaseFailure(e);
        }

        @Override
        public String message() {
            return "Database operation failed: " + cause.getMessage();
        }
    }

    /// Maps JPA exceptions to typed JpaError causes.
    ///
    /// @param throwable Exception to map
    ///
    /// @return Corresponding JpaError
    static JpaError fromException(Throwable throwable) {
        return switch (throwable) {
            case EntityNotFoundException _ -> EntityNotFound.INSTANCE;
            case NoResultException _ -> EntityNotFound.INSTANCE;
            case OptimisticLockException e -> new OptimisticLock(
                e.getEntity() != null ? e.getEntity().getClass().getSimpleName() : "Unknown",
                e.getEntity()
            );
            case PessimisticLockException e -> new PessimisticLock(e.getMessage());
            case LockTimeoutException e -> new PessimisticLock("Lock timeout: " + e.getMessage());
            case EntityExistsException e -> new EntityExists(
                "Entity",
                e.getMessage()
            );
            case QueryTimeoutException e -> new QueryTimeout(e.getMessage());
            case TransactionRequiredException _ -> TransactionRequired.INSTANCE;
            // Catch vendor-specific exceptions (e.g., Hibernate's ConstraintViolationException)
            case RuntimeException e -> DatabaseFailure.cause(e);
            default -> DatabaseFailure.cause(throwable);
        };
    }
}
