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

package org.pragmatica.jdbc;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/// Transaction aspect for JDBC operations.
/// Provides transactional boundaries with automatic commit/rollback.
public interface JdbcTransactional {
    /// Executes an operation within a transaction.
    /// Automatically commits on success and rolls back on failure.
    ///
    /// @param dataSource DataSource for connection acquisition
    /// @param operation Operation to execute with the connection
    /// @param <R> Result type
    ///
    /// @return Promise with operation result
    static <R> Promise<R> withTransaction(DataSource dataSource,
                                          Fn1<Promise<R>, Connection> operation) {
        return withTransaction(dataSource, JdbcError::fromException, operation);
    }

    /// Executes an operation within a transaction with custom error mapping.
    /// Automatically commits on success and rolls back on failure.
    ///
    /// @param dataSource DataSource for connection acquisition
    /// @param errorMapper Function to map exceptions to errors
    /// @param operation Operation to execute with the connection
    /// @param <R> Result type
    ///
    /// @return Promise with operation result
    static <R> Promise<R> withTransaction(DataSource dataSource,
                                          Fn1<JdbcError, Throwable> errorMapper,
                                          Fn1<Promise<R>, Connection> operation) {
        return Promise.promise(promise -> {
                                   Connection conn = null;
                                   try{
                                       conn = dataSource.getConnection();
                                       conn.setAutoCommit(false);
                                       var result = operation.apply(conn)
                                                             .await();
                                       if (result instanceof Result.Success<R>) {
                                           try{
                                               conn.commit();
                                           } catch (SQLException e) {
                                               rollback(conn);
                                               promise.resolve(errorMapper.apply(e)
                                                                          .result());
                                               return;
                                           }
                                       } else {
                                           rollback(conn);
                                       }
                                       promise.resolve(result);
                                   } catch (Exception e) {
                                       rollback(conn);
                                       promise.resolve(errorMapper.apply(e)
                                                                  .result());
                                   } finally{
                                       close(conn);
                                   }
                               });
    }

    /// Creates a transactional wrapper function.
    /// The returned function executes operations in a transaction.
    ///
    /// @param dataSource DataSource for connection acquisition
    /// @param <R> Result type
    ///
    /// @return Function that wraps operations in transactions
    static <R> Fn1<Promise<R>, Fn1<Promise<R>, Connection>> transactional(DataSource dataSource) {
        return operation -> withTransaction(dataSource, operation);
    }

    private static void rollback(Connection conn) {
        Option.option(conn)
              .onPresent(c -> {
                  try{
                      c.rollback();
                  } catch (SQLException _) {}
              });
    }

    private static void close(Connection conn) {
        Option.option(conn)
              .onPresent(c -> {
                  try{
                      c.setAutoCommit(true);
                      c.close();
                  } catch (SQLException _) {}
              });
    }
}
