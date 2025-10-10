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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Metamodel;

import java.util.List;
import java.util.Map;

/// Stub implementation of EntityManager for testing.
///
/// This stub is used instead of Mockito mocks because Mockito cannot mock Java 25 interfaces.
/// EntityManager extends AutoCloseable which comes from JDK.
///
/// Tests create instances of this stub and override only the methods they need using
/// anonymous classes, providing fine-grained control without mocking framework limitations.
public interface EntityManagerStub extends EntityManager {

    @Override
    default void persist(Object entity) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> T merge(T entity) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void remove(Object entity) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> T find(Class<T> entityClass, Object primaryKey) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> T find(EntityGraph<T> entityGraph, Object primaryKey, jakarta.persistence.FindOption... options) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> T find(Class<T> entityClass, Object primaryKey, jakarta.persistence.FindOption... options) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> T getReference(Class<T> entityClass, Object primaryKey) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> T getReference(T entity) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void flush() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void setFlushMode(FlushModeType flushMode) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default FlushModeType getFlushMode() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void lock(Object entity, LockModeType lockMode) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void lock(Object entity, LockModeType lockMode, jakarta.persistence.LockOption... options) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void refresh(Object entity) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void refresh(Object entity, Map<String, Object> properties) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void refresh(Object entity, LockModeType lockMode) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void refresh(Object entity, jakarta.persistence.RefreshOption... options) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void clear() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void detach(Object entity) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default boolean contains(Object entity) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default LockModeType getLockMode(Object entity) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void setProperty(String propertyName, Object value) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default Map<String, Object> getProperties() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default Query createQuery(String qlString) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> TypedQuery<T> createQuery(jakarta.persistence.criteria.CriteriaSelect<T> selectQuery) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default Query createQuery(CriteriaUpdate updateQuery) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default Query createQuery(CriteriaDelete deleteQuery) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> TypedQuery<T> createQuery(jakarta.persistence.TypedQueryReference<T> typedQueryReference) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default Query createNamedQuery(String name) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default Query createNativeQuery(String sqlString) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default Query createNativeQuery(String sqlString, Class resultClass) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default Query createNativeQuery(String sqlString, String resultSetMapping) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void joinTransaction() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default boolean isJoinedToTransaction() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> T unwrap(Class<T> cls) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default Object getDelegate() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void close() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default boolean isOpen() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default EntityTransaction getTransaction() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default EntityManagerFactory getEntityManagerFactory() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default CriteriaBuilder getCriteriaBuilder() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default Metamodel getMetamodel() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default EntityGraph<?> createEntityGraph(String graphName) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default EntityGraph<?> getEntityGraph(String graphName) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    default <T> T getDelegate(Class<T> cls) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <C, T> T callWithConnection(jakarta.persistence.ConnectionFunction<C, T> function) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default <C> void runWithConnection(jakarta.persistence.ConnectionConsumer<C> action) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void setCacheStoreMode(jakarta.persistence.CacheStoreMode cacheStoreMode) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default jakarta.persistence.CacheStoreMode getCacheStoreMode() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default void setCacheRetrieveMode(jakarta.persistence.CacheRetrieveMode cacheRetrieveMode) {
        throw new UnsupportedOperationException("Not implemented in stub");
    }

    @Override
    default jakarta.persistence.CacheRetrieveMode getCacheRetrieveMode() {
        throw new UnsupportedOperationException("Not implemented in stub");
    }
}
