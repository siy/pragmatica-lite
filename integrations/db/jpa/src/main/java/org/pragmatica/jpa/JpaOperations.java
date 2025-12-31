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

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

/// Functional wrapper around JPA EntityManager for Promise-based operations.
/// All operations use Promise.lift() to convert exceptions to typed Causes.
/// Designed for use in adapter leaves following JBCT patterns.
///
/// Usage:
/// ```java
/// var ops = JpaOperations.jpaOperations(entityManager);
///
/// // Query single result
/// ops.querySingle(JpaError::fromException, query)
///    .onSuccess(user -> processUser(user));
///
/// // Query optional result
/// ops.queryOptional(JpaError::fromException, query)
///    .onSuccess(optUser -> optUser.onPresent(this::processUser));
/// ```
public interface JpaOperations {
    /// Execute query returning single result.
    /// Fails if query returns no results or multiple results.
    ///
    /// @param errorMapper Maps exceptions to Cause
    /// @param query       Typed query to execute
    /// @param <T>         Result type
    ///
    /// @return Promise with single result or error
    <T> Promise<T> querySingle(Fn1<JpaError, Throwable> errorMapper, TypedQuery<T> query);

    /// Execute query returning optional result.
    /// Returns None if no results, Some(T) if one result.
    /// Fails if multiple results returned.
    ///
    /// @param errorMapper Maps exceptions to Cause
    /// @param query       Typed query to execute
    /// @param <T>         Result type
    ///
    /// @return Promise with optional result or error
    <T> Promise<Option<T>> queryOptional(Fn1<JpaError, Throwable> errorMapper, TypedQuery<T> query);

    /// Execute query returning list of results.
    ///
    /// @param errorMapper Maps exceptions to Cause
    /// @param query       Typed query to execute
    /// @param <T>         Result type
    ///
    /// @return Promise with result list or error
    <T> Promise<List<T>> queryList(Fn1<JpaError, Throwable> errorMapper, TypedQuery<T> query);

    /// Persist new entity.
    ///
    /// @param errorMapper Maps exceptions to Cause
    /// @param entity      Entity to persist
    /// @param <T>         Entity type
    ///
    /// @return Promise with persisted entity or error
    <T> Promise<T> persist(Fn1<JpaError, Throwable> errorMapper, T entity);

    /// Merge entity state.
    ///
    /// @param errorMapper Maps exceptions to Cause
    /// @param entity      Entity to merge
    /// @param <T>         Entity type
    ///
    /// @return Promise with merged entity or error
    <T> Promise<T> merge(Fn1<JpaError, Throwable> errorMapper, T entity);

    /// Remove entity.
    ///
    /// @param errorMapper Maps exceptions to Cause
    /// @param entity      Entity to remove
    /// @param <T>         Entity type
    ///
    /// @return Promise with Unit on success or error
    <T> Promise<Unit> remove(Fn1<JpaError, Throwable> errorMapper, T entity);

    /// Execute update or delete query.
    ///
    /// @param errorMapper Maps exceptions to Cause
    /// @param query       Query to execute
    ///
    /// @return Promise with count of affected rows or error
    Promise<Integer> executeUpdate(Fn1<JpaError, Throwable> errorMapper, Query query);

    /// Find entity by ID.
    ///
    /// @param errorMapper Maps exceptions to Cause
    /// @param entityClass Entity class
    /// @param id          Entity ID
    /// @param <T>         Entity type
    ///
    /// @return Promise with optional entity or error
    <T> Promise<Option<T>> findById(Fn1<JpaError, Throwable> errorMapper, Class<T> entityClass, Object id);

    /// Refresh entity from database.
    ///
    /// @param errorMapper Maps exceptions to Cause
    /// @param entity      Entity to refresh
    /// @param <T>         Entity type
    ///
    /// @return Promise with refreshed entity or error
    <T> Promise<T> refresh(Fn1<JpaError, Throwable> errorMapper, T entity);

    /// Flush pending changes to database.
    ///
    /// @param errorMapper Maps exceptions to Cause
    ///
    /// @return Promise with Unit on success or error
    Promise<Unit> flush(Fn1<JpaError, Throwable> errorMapper);

    /// Creates JpaOperations instance from EntityManager.
    ///
    /// @param em EntityManager instance
    ///
    /// @return JpaOperations wrapper
    static JpaOperations jpaOperations(EntityManager em) {
        return new JpaOperationsImpl(em);
    }
}
