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

package org.pragmatica.http.routing;

import org.pragmatica.http.error.HttpError;
import org.pragmatica.http.error.HttpStatusCause;
import org.pragmatica.http.model.HttpRequest;
import org.pragmatica.http.model.HttpResponse;
import org.pragmatica.http.model.ResponseBuilder;
import org.pragmatica.lang.Promise;

import java.util.Arrays;
import java.util.List;

/// HTTP router that matches requests to routes and executes handlers.
/// Automatically handles HttpStatusCause errors by converting them to HTTP responses.
public interface HttpRouter {
    /// Route an HTTP request to the appropriate handler.
    ///
    /// @param request HTTP request to route
    /// @return Promise containing HTTP response
    Promise<HttpResponse> route(HttpRequest request);

    /// Create a router from multiple routes.
    ///
    /// @param routes routes to register
    /// @return HttpRouter instance
    static HttpRouter router(RouteMatcher... routes) {
        return new DefaultHttpRouter(Arrays.asList(routes));
    }

    /// Create a router from a list of routes.
    ///
    /// @param routes routes to register
    /// @return HttpRouter instance
    static HttpRouter router(List<RouteMatcher> routes) {
        return new DefaultHttpRouter(routes);
    }
}

/// Default router implementation
record DefaultHttpRouter(List<RouteMatcher> routes) implements HttpRouter {
    @Override
    public Promise<HttpResponse> route(HttpRequest request) {
        return tryRoutes(request, 0);
    }

    private Promise<HttpResponse> tryRoutes(HttpRequest request, int index) {
        if (index >= routes.size()) {
            return Promise.success(HttpResponse.notFound(request.requestId()));
        }

        return routes.get(index)
            .match(request)
            .flatMap(optResponse -> ((org.pragmatica.lang.Option<HttpResponse>) optResponse)
                .map(Promise::success)
                .or(() -> tryRoutes(request, index + 1))
            )
            .recover(cause -> {
                if (cause instanceof HttpStatusCause httpCause) {
                    return ResponseBuilder.builder(request.requestId(), null)
                        .flatMap(httpCause::fillResponse)
                        .map(ResponseBuilder::build)
                        .fold(
                            _ -> HttpResponse.internalServerError(request.requestId()),
                            response -> response
                        );
                }
                return HttpResponse.internalServerError(request.requestId());
            });
    }
}
