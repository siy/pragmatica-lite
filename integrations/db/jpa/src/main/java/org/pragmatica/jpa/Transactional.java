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

import org.pragmatica.lang.Functions.Fn0;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Aspect decorator for JPA transaction management.
/// Wraps operations with transaction begin/commit/rollback lifecycle.
///
/// Usage:
/// ```java
/// var decorated = Transactional.withTransaction(
///     entityManager,
///     JpaError::fromException,
///     userRepository::createUser
/// );
///
/// decorated.apply(request)
///     .onSuccess(user -> log.info("Created user: {}", user.id()));
///```
public interface Transactional {
    Logger log = LoggerFactory.getLogger(Transactional.class);

    /// Decorates a function with transaction management.
    /// Transaction is committed on success, rolled back on failure.
    ///
    /// @param em          EntityManager to manage transaction
    /// @param errorMapper Maps exceptions to Cause
    /// @param operation   Function to execute within transaction
    /// @param <I>         Input type
    /// @param <O>         Output type
    ///
    /// @return Decorated function that runs within the transaction
    static <I, O> Fn1<Promise<O>, I> withTransaction(EntityManager em,
                                                     Fn1<JpaError, Throwable> errorMapper,
                                                     Fn1<Promise<O>, I> operation) {
        return input -> {
            var tx = em.getTransaction();
            return beginTransaction(errorMapper, tx, input)
                                   .flatMap(operation)
                                   .onFailure(_ -> handleRollback(tx))
                                   .flatMap(result -> handleCommit(errorMapper, tx, result));
        };
    }

    /// Decorates a supplier (no-argument function) with transaction management.
    ///
    /// @param em          EntityManager to manage transaction
    /// @param errorMapper Maps exceptions to Cause
    /// @param operation   Supplier to execute within transaction
    /// @param <O>         Output type
    ///
    /// @return Decorated supplier that runs within the transaction
    static <O> Fn0<Promise<O>> withTransaction(EntityManager em,
                                               Fn1<JpaError, Throwable> errorMapper,
                                               Fn0<Promise<O>> operation) {
        return () -> withTransaction(em,
                                     errorMapper,
                                     _ -> operation.apply())
                                    .apply(Unit.unit());
    }

    private static <T> Promise<T> beginTransaction(Fn1<JpaError, Throwable> errorMapper,
                                                   EntityTransaction tx,
                                                   T input) {
        try{
            tx.begin();
            return Promise.success(input);
        } catch (Exception e) {
            return Promise.failure(errorMapper.apply(e));
        }
    }

    private static void handleRollback(EntityTransaction tx) {
        if (tx.isActive()) {
            try{
                tx.rollback();
            } catch (Exception rollbackEx) {
                log.error("Failed to rollback transaction", rollbackEx);
            }
        }
    }

    private static <O> Promise<O> handleCommit(Fn1<JpaError, Throwable> errorMapper, EntityTransaction tx, O result) {
        try{
            tx.commit();
            return Promise.success(result);
        } catch (Exception e) {
            if (tx.isActive()) {
                try{
                    tx.rollback();
                } catch (Exception rollbackEx) {
                    log.error("Failed to rollback transaction after commit failure", rollbackEx);
                }
            }
            return Promise.failure(errorMapper.apply(e));
        }
    }
}
