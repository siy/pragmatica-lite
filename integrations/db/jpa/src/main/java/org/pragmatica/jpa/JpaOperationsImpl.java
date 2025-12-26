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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.List;

/// Implementation of JpaOperations wrapping EntityManager operations.
record JpaOperationsImpl(EntityManager em) implements JpaOperations {

    @Override
    public <T> Promise<T> querySingle(Fn1<JpaError, Throwable> errorMapper, TypedQuery<T> query) {
        return Promise.lift(errorMapper, query::getSingleResult);
    }

    @Override
    public <T> Promise<Option<T>> queryOptional(Fn1<JpaError, Throwable> errorMapper, TypedQuery<T> query) {
        return Promise.lift(
            errorMapper,
            () -> {
                var results = query.setMaxResults(2).getResultList();
                if (results.isEmpty()) {
                    return Option.none();
                }
                if (results.size() > 1) {
                    throw new jakarta.persistence.NonUniqueResultException("Query returned more than one result");
                }
                return Option.option(results.getFirst());
            }
        );
    }

    @Override
    public <T> Promise<List<T>> queryList(Fn1<JpaError, Throwable> errorMapper, TypedQuery<T> query) {
        return Promise.lift(errorMapper, query::getResultList);
    }

    @Override
    public <T> Promise<T> persist(Fn1<JpaError, Throwable> errorMapper, T entity) {
        return Promise.lift(errorMapper, () -> {
            em.persist(entity);
            return entity;
        });
    }

    @Override
    public <T> Promise<T> merge(Fn1<JpaError, Throwable> errorMapper, T entity) {
        return Promise.lift(errorMapper, () -> em.merge(entity));
    }

    @Override
    public <T> Promise<Unit> remove(Fn1<JpaError, Throwable> errorMapper, T entity) {
        return Promise.lift(errorMapper, () -> {
            em.remove(entity);
            return Unit.unit();
        });
    }

    @Override
    public Promise<Integer> executeUpdate(Fn1<JpaError, Throwable> errorMapper, Query query) {
        return Promise.lift(errorMapper, query::executeUpdate);
    }

    @Override
    public <T> Promise<Option<T>> findById(Fn1<JpaError, Throwable> errorMapper, Class<T> entityClass, Object id) {
        return Promise.lift(
            errorMapper,
            () -> Option.option(em.find(entityClass, id))
        );
    }

    @Override
    public <T> Promise<T> refresh(Fn1<JpaError, Throwable> errorMapper, T entity) {
        return Promise.lift(errorMapper, () -> {
            em.refresh(entity);
            return entity;
        });
    }

    @Override
    public Promise<Unit> flush(Fn1<JpaError, Throwable> errorMapper) {
        return Promise.lift(errorMapper, () -> {
            em.flush();
            return Unit.unit();
        });
    }
}
