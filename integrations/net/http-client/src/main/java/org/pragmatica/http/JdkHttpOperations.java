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

package org.pragmatica.http;

import org.pragmatica.lang.Promise;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.concurrent.Executor;

/// JDK HttpClient-based implementation of HttpOperations.
/// Bridges CompletableFuture to Promise for seamless integration.
public final class JdkHttpOperations implements HttpOperations {
    private final HttpClient client;

    private JdkHttpOperations(HttpClient client) {
        this.client = client;
    }

    /// Creates JdkHttpOperations with default HttpClient.
    ///
    /// @return JdkHttpOperations instance with default configuration
    public static JdkHttpOperations jdkHttpOperations() {
        return new JdkHttpOperations(HttpClient.newHttpClient());
    }

    /// Creates JdkHttpOperations with custom HttpClient.
    ///
    /// @param client Custom HttpClient instance
    ///
    /// @return JdkHttpOperations instance wrapping the provided client
    public static JdkHttpOperations jdkHttpOperations(HttpClient client) {
        return new JdkHttpOperations(client);
    }

    /// Creates JdkHttpOperations with common configuration options.
    ///
    /// @param connectTimeout Connection timeout
    /// @param followRedirects Redirect policy
    /// @param executor Custom executor for async operations
    ///
    /// @return JdkHttpOperations instance with specified configuration
    public static JdkHttpOperations jdkHttpOperations(Duration connectTimeout,
                                                       HttpClient.Redirect followRedirects,
                                                       Executor executor) {
        var builder = HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .followRedirects(followRedirects);

        if (executor != null) {
            builder.executor(executor);
        }

        return new JdkHttpOperations(builder.build());
    }

    @Override
    public <T> Promise<HttpResult<T>> send(HttpRequest request, BodyHandler<T> handler) {
        return Promise.promise(promise ->
            client.sendAsync(request, handler)
                .thenApply(HttpResult::from)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        promise.fail(HttpError.fromException(error));
                    } else {
                        promise.succeed(result);
                    }
                })
        );
    }

    /// Returns the underlying HttpClient.
    ///
    /// @return The wrapped HttpClient instance
    public HttpClient client() {
        return client;
    }
}
