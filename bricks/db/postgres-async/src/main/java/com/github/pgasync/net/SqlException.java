/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pgasync.net;

import com.github.pgasync.SqlError.ServerError;

import java.io.Serial;

/**
 * Backend or client error. If the error is sent by backend, SQLSTATE error code
 * is available.
 *
 * @author Antti Laisi
 */
public class SqlException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ServerError serverError;

    public SqlException(ServerError serverError) {
        super(serverError.message());
        this.serverError = serverError;
    }

    public SqlException(String message) {
        super(message);
        this.serverError = null;
    }

    public ServerError error() {
        return serverError;
    }
}
