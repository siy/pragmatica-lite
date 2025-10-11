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

    /// Extracts entity ID using reflection, handling common JPA ID patterns.
    private static Object extractEntityId(Object entity) {
        try {
            // Try getId() method first (most common)
            var method = entity.getClass().getMethod("getId");
            return method.invoke(entity);
        } catch (Exception _) {
            // Try id() method (for records)
            try {
                var method = entity.getClass().getMethod("id");
                return method.invoke(entity);
            } catch (Exception __) {
                return "Unknown";
            }
        }
    }

    /// Maps JPA exceptions to typed JpaError causes.
    ///
    /// Note: ConstraintViolation errors are vendor-specific. The JPA spec does not define
    /// a ConstraintViolationException. Vendors like Hibernate throw implementation-specific
    /// exceptions (org.hibernate.exception.ConstraintViolationException) that cannot be
    /// caught without adding vendor dependencies. These fall through to DatabaseFailure.
    ///
    /// @param throwable Exception to map
    ///
    /// @return Corresponding JpaError
    static JpaError fromException(Throwable throwable) {
        return switch (throwable) {
            case EntityNotFoundException _ -> EntityNotFound.INSTANCE;
            case NoResultException _ -> EntityNotFound.INSTANCE;
            case OptimisticLockException e -> {
                var entity = e.getEntity();
                var entityType = entity != null ? entity.getClass().getSimpleName() : "Unknown";
                var entityId = entity != null ? extractEntityId(entity) : "Unknown";
                yield new OptimisticLock(entityType, entityId);
            }
            case PessimisticLockException e -> new PessimisticLock(e.getMessage());
            case LockTimeoutException e -> new PessimisticLock("Lock timeout: " + e.getMessage());
            case EntityExistsException e -> {
                // EntityExistsException doesn't provide entity details, only message
                var message = e.getMessage() != null ? e.getMessage() : "Unknown";
                yield new EntityExists("Entity", message);
            }
            case QueryTimeoutException e -> new QueryTimeout(e.getMessage());
            case TransactionRequiredException _ -> TransactionRequired.INSTANCE;
            // Vendor-specific exceptions (Hibernate, EclipseLink) fall through to DatabaseFailure
            // Cannot catch without vendor dependencies, keeping JPA integration vendor-neutral
            case RuntimeException e -> DatabaseFailure.cause(e);
            default -> DatabaseFailure.cause(throwable);
        };
    }
}
