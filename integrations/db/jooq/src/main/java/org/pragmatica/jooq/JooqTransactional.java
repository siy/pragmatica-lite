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

package org.pragmatica.jooq;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Transaction aspect for JOOQ operations.
/// Provides automatic transaction management with commit on success and rollback on failure.
public interface JooqTransactional {
    /// Executes a function within a transaction.
    ///
    /// @param operation Function that receives JooqOperations and returns a Promise
    /// @param <T> Result type
    ///
    /// @return Promise containing the operation result
    <T> Promise<T> withTransaction(Fn1<Promise<T>, JooqOperations> operation);

    /// Creates a JooqTransactional from a DataSource.
    ///
    /// @param dataSource JDBC DataSource
    /// @param dialect SQL dialect
    ///
    /// @return JooqTransactional instance
    static JooqTransactional jooqTransactional(DataSource dataSource, SQLDialect dialect) {
        return new DataSourceJooqTransactional(dataSource, dialect);
    }

    /// Creates a JooqTransactional from a DataSource with default dialect.
    ///
    /// @param dataSource JDBC DataSource
    ///
    /// @return JooqTransactional instance
    static JooqTransactional jooqTransactional(DataSource dataSource) {
        return jooqTransactional(dataSource, SQLDialect.DEFAULT);
    }
}

/// DataSource-based implementation of JooqTransactional.
final class DataSourceJooqTransactional implements JooqTransactional {
    private static final Logger LOG = LoggerFactory.getLogger(DataSourceJooqTransactional.class);

    private final DataSource dataSource;
    private final SQLDialect dialect;

    DataSourceJooqTransactional(DataSource dataSource, SQLDialect dialect) {
        this.dataSource = dataSource;
        this.dialect = dialect;
    }

    @Override
    public <T> Promise<T> withTransaction(Fn1<Promise<T>, JooqOperations> operation) {
        return Promise.lift(JooqError::fromException,
                            () -> {
                                Connection conn = null;
                                try{
                                    conn = dataSource.getConnection();
                                    conn.setAutoCommit(false);
                                    var ops = new ConnectionJooqOperations(conn, dialect);
                                    var result = operation.apply(ops)
                                                          .await();
                                    var connection = conn;
                                    return result.fold(cause -> {
                                                           rollback(connection);
                                                           throw new TransactionFailedException(cause);
                                                       },
                                                       value -> {
                                                           commit(connection);
                                                           return value;
                                                       });
                                } catch (TransactionFailedException e) {
                                    throw e;
                                } catch (Exception e) {
                                    rollback(conn);
                                    throw e;
                                } finally{
                                    close(conn);
                                }
                            });
    }

    private void commit(Connection conn) {
        if (conn != null) {
            try{
                conn.commit();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to commit transaction", e);
            }
        }
    }

    private void rollback(Connection conn) {
        if (conn != null) {
            try{
                conn.rollback();
            } catch (SQLException e) {
                LOG.error("Failed to rollback transaction", e);
            }
        }
    }

    private void close(Connection conn) {
        if (conn != null) {
            try{
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                LOG.warn("Failed to restore autoCommit", e);
            }
            try{
                conn.close();
            } catch (SQLException e) {
                LOG.error("Failed to close connection", e);
            }
        }
    }
}

/// JooqOperations implementation that uses a single connection (for transactions).
final class ConnectionJooqOperations implements JooqOperations {
    private final Connection connection;
    private final SQLDialect dialect;
    private final DSLContext dsl;

    ConnectionJooqOperations(Connection connection, SQLDialect dialect) {
        this.connection = connection;
        this.dialect = dialect;
        this.dsl = DSL.using(connection, dialect);
    }

    @Override
    public <R extends Record> Promise<R> fetchOne(ResultQuery<R> query) {
        return Promise.lift(e -> JooqError.fromException(e, query.getSQL()),
                            () -> {
                                var result = dsl.fetch(query);
                                if (result.isEmpty()) {
                                    throw new JooqNoResultException("Query returned no results");
                                }
                                if (result.size() > 1) {
                                    throw new JooqMultipleResultsException("Query returned " + result.size()
                                                                           + " results",
                                                                           result.size());
                                }
                                return result.get(0);
                            });
    }

    @Override
    public <R extends Record> Promise<Option<R>> fetchOptional(ResultQuery<R> query) {
        return Promise.lift(e -> JooqError.fromException(e, query.getSQL()),
                            () -> {
                                var result = dsl.fetch(query);
                                return result.isEmpty()
                                       ? Option.none()
                                       : Option.option(result.get(0));
                            });
    }

    @Override
    public <R extends Record> Promise<List<R>> fetch(ResultQuery<R> query) {
        return Promise.lift(e -> JooqError.fromException(e, query.getSQL()),
                            () -> dsl.fetch(query));
    }

    @Override
    public Promise<Integer> execute(Query query) {
        return Promise.lift(e -> JooqError.fromException(e, query.getSQL()),
                            () -> dsl.execute(query));
    }

    @Override
    public DSLContext dsl() {
        return dsl;
    }
}

/// Exception wrapper for propagating Cause through transaction boundary.
final class TransactionFailedException extends RuntimeException {
    private final org.pragmatica.lang.Cause cause;

    TransactionFailedException(org.pragmatica.lang.Cause cause) {
        super(cause.message());
        this.cause = cause;
    }

    org.pragmatica.lang.Cause causeValue() {
        return cause;
    }
}
