package org.pragmatica.http.routing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.http.routing.HttpMethod.*;
import static org.pragmatica.http.routing.PathParameter.*;
import static org.pragmatica.http.routing.QueryParameter.*;
import static org.pragmatica.http.routing.Route.*;

class RouteBuilderTest {

    record TestRequest(String name, int value) {}
    record TestResponse(String result) {}

    // ===================================================================================
    // No Parameters
    // ===================================================================================
    @Nested
    class NoParameters {
        @Test
        void no_parameters_get() {
            Route<TestResponse> route = Route.<TestResponse>get("/health")
                .to(_ -> Promise.success(new TestResponse("OK")))
                .asJson();

            assertThat(route.method()).isEqualTo(GET);
            assertThat(route.path()).isEqualTo("/health/");
            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }

        @Test
        void no_parameters_post() {
            Route<TestResponse> route = Route.<TestResponse>post("/action")
                .to(_ -> Promise.success(new TestResponse("Done")))
                .asText();

            assertThat(route.method()).isEqualTo(POST);
            assertThat(route.path()).isEqualTo("/action/");
            assertThat(route.contentType()).isEqualTo(CommonContentTypes.TEXT_PLAIN);
        }

        @Test
        void no_parameters_withoutParameters_explicit() {
            Route<TestResponse> route = Route.<TestResponse>get("/status")
                .withoutParameters()
                .to(_ -> Promise.success(new TestResponse("Running")))
                .asJson();

            assertThat(route.method()).isEqualTo(GET);
            assertThat(route.path()).isEqualTo("/status/");
        }

        @Test
        void toJson_convenience() {
            Route<TestResponse> route = Route.<TestResponse>get("/test")
                .toJson(_ -> Promise.success(new TestResponse("Value")));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }

        @Test
        void toText_convenience() {
            Route<TestResponse> route = Route.<TestResponse>get("/test")
                .toText(_ -> Promise.success(new TestResponse("Value")));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.TEXT_PLAIN);
        }

        @Test
        void toJson_supplier_convenience() {
            Route<TestResponse> route = Route.<TestResponse>get("/test")
                .toJson(() -> new TestResponse("Constant"));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }

        @Test
        void toText_supplier_convenience() {
            Route<TestResponse> route = Route.<TestResponse>get("/test")
                .toText(() -> new TestResponse("Constant"));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.TEXT_PLAIN);
        }
    }

    // ===================================================================================
    // Path Parameters Only
    // ===================================================================================
    @Nested
    class PathOnlyBuilders {
        @Test
        void single_path_parameter() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/")
                .withPath(aLong())
                .to(id -> Promise.success(new TestResponse("User " + id)))
                .asJson();

            assertThat(route.method()).isEqualTo(GET);
            assertThat(route.path()).isEqualTo("/users/");
        }

        @Test
        void two_path_parameters() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders/")
                .withPath(aLong(), aLong())
                .to((userId, orderId) -> Promise.success(new TestResponse("Order " + orderId + " for user " + userId)))
                .asJson();

            assertThat(route.method()).isEqualTo(GET);
            assertThat(route.path()).isEqualTo("/users/orders/");
        }

        @Test
        void three_path_parameters() {
            Route<TestResponse> route = Route.<TestResponse>get("/org/proj/task/")
                .withPath(aString(), aString(), aLong())
                .to((org, proj, taskId) -> Promise.success(new TestResponse(org + "/" + proj + "/" + taskId)))
                .asJson();

            assertThat(route).isNotNull();
            assertThat(route.path()).isEqualTo("/org/proj/task/");
        }

        @Test
        void four_path_parameters() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/d/")
                .withPath(aString(), aString(), aString(), aInteger())
                .to((a, b, c, d) -> Promise.success(new TestResponse(a + b + c + d)))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void five_path_parameters() {
            Route<TestResponse> route = Route.<TestResponse>get("/v1/a/b/c/d/e/")
                .withPath(aString(), aString(), aString(), aString(), aLong())
                .to((a, b, c, d, e) -> Promise.success(new TestResponse("5 params")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void path_toResult_variant() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/")
                .withPath(aLong())
                .toResult(id -> Result.success(new TestResponse("User " + id)))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void path_toValue_variant() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/")
                .withPath(aLong())
                .toValue(id -> new TestResponse("User " + id))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_path_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders/")
                .withPath(aLong(), aLong())
                .toResult((userId, orderId) -> Result.success(new TestResponse("Order")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_path_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders/")
                .withPath(aLong(), aLong())
                .toValue((userId, orderId) -> new TestResponse("Order"))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_path_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/")
                .withPath(aString(), aString(), aLong())
                .toResult((a, b, c) -> Result.success(new TestResponse("Three")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_path_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/")
                .withPath(aString(), aString(), aLong())
                .toValue((a, b, c) -> new TestResponse("Three"))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void four_path_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/d/")
                .withPath(aString(), aString(), aString(), aLong())
                .toResult((a, b, c, d) -> Result.success(new TestResponse("Four")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void four_path_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/d/")
                .withPath(aString(), aString(), aString(), aLong())
                .toValue((a, b, c, d) -> new TestResponse("Four"))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void five_path_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/d/e/")
                .withPath(aString(), aString(), aString(), aString(), aLong())
                .toResult((a, b, c, d, e) -> Result.success(new TestResponse("Five")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void five_path_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/d/e/")
                .withPath(aString(), aString(), aString(), aString(), aLong())
                .toValue((a, b, c, d, e) -> new TestResponse("Five"))
                .asJson();

            assertThat(route).isNotNull();
        }
    }

    // ===================================================================================
    // Body Only
    // ===================================================================================
    @Nested
    class BodyOnlyBuilder {
        @Test
        void body_parameter_class() {
            Route<TestResponse> route = Route.<TestResponse>post("/users")
                .withBody(TestRequest.class)
                .to(request -> Promise.success(new TestResponse(request.name())))
                .asJson();

            assertThat(route.method()).isEqualTo(POST);
            assertThat(route.path()).isEqualTo("/users/");
        }

        @Test
        void body_toResult() {
            Route<TestResponse> route = Route.<TestResponse>post("/users")
                .withBody(TestRequest.class)
                .toResult(request -> Result.success(new TestResponse(request.name())))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void body_toJson_convenience() {
            Route<TestResponse> route = Route.<TestResponse>post("/users")
                .withBody(TestRequest.class)
                .toJson(request -> Promise.success(new TestResponse(request.name())));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }
    }

    // ===================================================================================
    // Query Parameters Only
    // ===================================================================================
    @Nested
    class QueryOnlyBuilders {
        @Test
        void single_query_parameter() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"))
                .to(query -> Promise.success(new TestResponse(query.or("default"))))
                .asJson();

            assertThat(route).isNotNull();
            assertThat(route.method()).isEqualTo(GET);
        }

        @Test
        void two_query_parameters() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"), aInteger("page"))
                .to((query, page) -> Promise.success(new TestResponse("Search")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_query_parameters() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"))
                .to((query, page, size) -> Promise.success(new TestResponse("Search")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void four_query_parameters() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"))
                .to((query, page, size, asc) -> Promise.success(new TestResponse("Search")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void five_query_parameters() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"), aString("sort"))
                .to((query, page, size, asc, sort) -> Promise.success(new TestResponse("Search")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"))
                .toResult(query -> Result.success(new TestResponse(query.or("default"))))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"))
                .toValue(query -> new TestResponse(query.or("default")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"), aInteger("page"))
                .toResult((q, page) -> Result.success(new TestResponse("result")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"), aInteger("page"))
                .toValue((q, page) -> new TestResponse("result"))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"))
                .toResult((q, page, size) -> Result.success(new TestResponse("result")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"))
                .toValue((q, page, size) -> new TestResponse("result"))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void four_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"))
                .toResult((q, page, size, asc) -> Result.success(new TestResponse("result")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void four_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"))
                .toValue((q, page, size, asc) -> new TestResponse("result"))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void five_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"), aString("sort"))
                .toResult((q, page, size, asc, sort) -> Result.success(new TestResponse("result")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void five_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"), aString("sort"))
                .toValue((q, page, size, asc, sort) -> new TestResponse("result"))
                .asJson();

            assertThat(route).isNotNull();
        }
    }

    // ===================================================================================
    // Path + Body Combinations
    // ===================================================================================
    @Nested
    class PathPlusBodyBuilders {
        @Test
        void one_path_and_body() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/")
                .withPath(aLong())
                .withBody(TestRequest.class)
                .to((id, body) -> Promise.success(new TestResponse("Updated " + id)))
                .asJson();

            assertThat(route.method()).isEqualTo(PUT);
        }

        @Test
        void one_path_body_toResult() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/")
                .withPath(aLong())
                .withBody(TestRequest.class)
                .toResult((id, body) -> Result.success(new TestResponse("Updated " + id)))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_body_toJson() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/")
                .withPath(aLong())
                .withBody(TestRequest.class)
                .toJson((id, body) -> Promise.success(new TestResponse("Updated " + id)));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }

        @Test
        void two_path_and_body() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/orders/")
                .withPath(aLong(), aLong())
                .withBody(TestRequest.class)
                .to((userId, orderId, body) -> Promise.success(new TestResponse("Updated order")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_path_body_toResult() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/orders/")
                .withPath(aLong(), aLong())
                .withBody(TestRequest.class)
                .toResult((userId, orderId, body) -> Result.success(new TestResponse("Updated order")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_path_body_toJson() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/orders/")
                .withPath(aLong(), aLong())
                .withBody(TestRequest.class)
                .toJson((userId, orderId, body) -> Promise.success(new TestResponse("Updated order")));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }

        @Test
        void three_path_and_body() {
            Route<TestResponse> route = Route.<TestResponse>put("/a/b/c/")
                .withPath(aString(), aString(), aLong())
                .withBody(TestRequest.class)
                .to((a, b, c, body) -> Promise.success(new TestResponse("Three path + body")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_path_body_toResult() {
            Route<TestResponse> route = Route.<TestResponse>put("/a/b/c/")
                .withPath(aString(), aString(), aLong())
                .withBody(TestRequest.class)
                .toResult((a, b, c, body) -> Result.success(new TestResponse("Three path + body")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_path_body_toJson() {
            Route<TestResponse> route = Route.<TestResponse>put("/a/b/c/")
                .withPath(aString(), aString(), aLong())
                .withBody(TestRequest.class)
                .toJson((a, b, c, body) -> Promise.success(new TestResponse("Three path + body")));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }

        @Test
        void four_path_and_body() {
            Route<TestResponse> route = Route.<TestResponse>put("/a/b/c/d/")
                .withPath(aString(), aString(), aString(), aLong())
                .withBody(TestRequest.class)
                .to((a, b, c, d, body) -> Promise.success(new TestResponse("Four path + body")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void four_path_body_toResult() {
            Route<TestResponse> route = Route.<TestResponse>put("/a/b/c/d/")
                .withPath(aString(), aString(), aString(), aLong())
                .withBody(TestRequest.class)
                .toResult((a, b, c, d, body) -> Result.success(new TestResponse("Four path + body")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void four_path_body_toJson() {
            Route<TestResponse> route = Route.<TestResponse>put("/a/b/c/d/")
                .withPath(aString(), aString(), aString(), aLong())
                .withBody(TestRequest.class)
                .toJson((a, b, c, d, body) -> Promise.success(new TestResponse("Four path + body")));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }
    }

    // ===================================================================================
    // Query + Body Combinations
    // ===================================================================================
    @Nested
    class QueryPlusBodyBuilders {
        @Test
        void one_query_and_body() {
            Route<TestResponse> route = Route.<TestResponse>post("/search")
                .withQuery(aString("filter"))
                .withBody(TestRequest.class)
                .to((filter, body) -> Promise.success(new TestResponse("Filtered")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_query_body_toResult() {
            Route<TestResponse> route = Route.<TestResponse>post("/search")
                .withQuery(aString("filter"))
                .withBody(TestRequest.class)
                .toResult((filter, body) -> Result.success(new TestResponse("Filtered")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_query_body_toJson() {
            Route<TestResponse> route = Route.<TestResponse>post("/search")
                .withQuery(aString("filter"))
                .withBody(TestRequest.class)
                .toJson((filter, body) -> Promise.success(new TestResponse("Filtered")));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }

        @Test
        void two_query_and_body() {
            Route<TestResponse> route = Route.<TestResponse>post("/search")
                .withQuery(aString("filter"), aInteger("limit"))
                .withBody(TestRequest.class)
                .to((filter, limit, body) -> Promise.success(new TestResponse("Two query + body")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_query_body_toResult() {
            Route<TestResponse> route = Route.<TestResponse>post("/search")
                .withQuery(aString("filter"), aInteger("limit"))
                .withBody(TestRequest.class)
                .toResult((filter, limit, body) -> Result.success(new TestResponse("Two query + body")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_query_body_toJson() {
            Route<TestResponse> route = Route.<TestResponse>post("/search")
                .withQuery(aString("filter"), aInteger("limit"))
                .withBody(TestRequest.class)
                .toJson((filter, limit, body) -> Promise.success(new TestResponse("Two query + body")));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }

        @Test
        void three_query_and_body() {
            Route<TestResponse> route = Route.<TestResponse>post("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"))
                .withBody(TestRequest.class)
                .to((q, page, size, body) -> Promise.success(new TestResponse("Three query + body")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_query_body_toResult() {
            Route<TestResponse> route = Route.<TestResponse>post("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"))
                .withBody(TestRequest.class)
                .toResult((q, page, size, body) -> Result.success(new TestResponse("Three query + body")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_query_body_toJson() {
            Route<TestResponse> route = Route.<TestResponse>post("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"))
                .withBody(TestRequest.class)
                .toJson((q, page, size, body) -> Promise.success(new TestResponse("Three query + body")));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }

        @Test
        void four_query_and_body() {
            Route<TestResponse> route = Route.<TestResponse>post("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"))
                .withBody(TestRequest.class)
                .to((q, page, size, asc, body) -> Promise.success(new TestResponse("Four query + body")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void four_query_body_toResult() {
            Route<TestResponse> route = Route.<TestResponse>post("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"))
                .withBody(TestRequest.class)
                .toResult((q, page, size, asc, body) -> Result.success(new TestResponse("Four query + body")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void four_query_body_toJson() {
            Route<TestResponse> route = Route.<TestResponse>post("/search")
                .withQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"))
                .withBody(TestRequest.class)
                .toJson((q, page, size, asc, body) -> Promise.success(new TestResponse("Four query + body")));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }
    }

    // ===================================================================================
    // Path + Query Combinations
    // ===================================================================================
    @Nested
    class PathPlusQueryBuilders {
        // 1 path + 1 query
        @Test
        void one_path_one_query() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders")
                .withPath(aLong())
                .withQuery(aString("status"))
                .to((userId, status) -> Promise.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_one_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders")
                .withPath(aLong())
                .withQuery(aString("status"))
                .toResult((userId, status) -> Result.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_one_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders")
                .withPath(aLong())
                .withQuery(aString("status"))
                .toValue((userId, status) -> new TestResponse("Found"))
                .asJson();

            assertThat(route).isNotNull();
        }

        // 1 path + 2 query
        @Test
        void one_path_two_query() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders")
                .withPath(aLong())
                .withQuery(aString("status"), aInteger("page"))
                .to((userId, status, page) -> Promise.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_two_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders")
                .withPath(aLong())
                .withQuery(aString("status"), aInteger("page"))
                .toResult((userId, status, page) -> Result.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_two_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders")
                .withPath(aLong())
                .withQuery(aString("status"), aInteger("page"))
                .toValue((userId, status, page) -> new TestResponse("Found"))
                .asJson();

            assertThat(route).isNotNull();
        }

        // 1 path + 3 query
        @Test
        void one_path_three_query() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders")
                .withPath(aLong())
                .withQuery(aString("status"), aInteger("page"), aInteger("size"))
                .to((userId, status, page, size) -> Promise.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_three_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders")
                .withPath(aLong())
                .withQuery(aString("status"), aInteger("page"), aInteger("size"))
                .toResult((userId, status, page, size) -> Result.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_three_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders")
                .withPath(aLong())
                .withQuery(aString("status"), aInteger("page"), aInteger("size"))
                .toValue((userId, status, page, size) -> new TestResponse("Found"))
                .asJson();

            assertThat(route).isNotNull();
        }

        // 1 path + 4 query
        @Test
        void one_path_four_query() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders")
                .withPath(aLong())
                .withQuery(aString("status"), aInteger("page"), aInteger("size"), aBoolean("asc"))
                .to((userId, status, page, size, asc) -> Promise.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_four_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders")
                .withPath(aLong())
                .withQuery(aString("status"), aInteger("page"), aInteger("size"), aBoolean("asc"))
                .toResult((userId, status, page, size, asc) -> Result.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_four_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders")
                .withPath(aLong())
                .withQuery(aString("status"), aInteger("page"), aInteger("size"), aBoolean("asc"))
                .toValue((userId, status, page, size, asc) -> new TestResponse("Found"))
                .asJson();

            assertThat(route).isNotNull();
        }

        // 2 path + 1 query
        @Test
        void two_path_one_query() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders/")
                .withPath(aLong(), aLong())
                .withQuery(aString("expand"))
                .to((userId, orderId, expand) -> Promise.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_path_one_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders/")
                .withPath(aLong(), aLong())
                .withQuery(aString("expand"))
                .toResult((userId, orderId, expand) -> Result.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_path_one_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders/")
                .withPath(aLong(), aLong())
                .withQuery(aString("expand"))
                .toValue((userId, orderId, expand) -> new TestResponse("Found"))
                .asJson();

            assertThat(route).isNotNull();
        }

        // 2 path + 2 query
        @Test
        void two_path_two_query() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders/")
                .withPath(aLong(), aLong())
                .withQuery(aString("expand"), aBoolean("includeDeleted"))
                .to((userId, orderId, expand, includeDeleted) -> Promise.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_path_two_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders/")
                .withPath(aLong(), aLong())
                .withQuery(aString("expand"), aBoolean("includeDeleted"))
                .toResult((userId, orderId, expand, includeDeleted) -> Result.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_path_two_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders/")
                .withPath(aLong(), aLong())
                .withQuery(aString("expand"), aBoolean("includeDeleted"))
                .toValue((userId, orderId, expand, includeDeleted) -> new TestResponse("Found"))
                .asJson();

            assertThat(route).isNotNull();
        }

        // 2 path + 3 query
        @Test
        void two_path_three_query() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders/")
                .withPath(aLong(), aLong())
                .withQuery(aString("expand"), aBoolean("includeDeleted"), aString("format"))
                .to((userId, orderId, expand, includeDeleted, format) -> Promise.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_path_three_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders/")
                .withPath(aLong(), aLong())
                .withQuery(aString("expand"), aBoolean("includeDeleted"), aString("format"))
                .toResult((userId, orderId, expand, includeDeleted, format) -> Result.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_path_three_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/orders/")
                .withPath(aLong(), aLong())
                .withQuery(aString("expand"), aBoolean("includeDeleted"), aString("format"))
                .toValue((userId, orderId, expand, includeDeleted, format) -> new TestResponse("Found"))
                .asJson();

            assertThat(route).isNotNull();
        }

        // 3 path + 1 query
        @Test
        void three_path_one_query() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/")
                .withPath(aString(), aString(), aLong())
                .withQuery(aString("expand"))
                .to((a, b, c, expand) -> Promise.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_path_one_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/")
                .withPath(aString(), aString(), aLong())
                .withQuery(aString("expand"))
                .toResult((a, b, c, expand) -> Result.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_path_one_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/")
                .withPath(aString(), aString(), aLong())
                .withQuery(aString("expand"))
                .toValue((a, b, c, expand) -> new TestResponse("Found"))
                .asJson();

            assertThat(route).isNotNull();
        }

        // 3 path + 2 query
        @Test
        void three_path_two_query() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/")
                .withPath(aString(), aString(), aLong())
                .withQuery(aString("expand"), aBoolean("verbose"))
                .to((a, b, c, expand, verbose) -> Promise.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_path_two_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/")
                .withPath(aString(), aString(), aLong())
                .withQuery(aString("expand"), aBoolean("verbose"))
                .toResult((a, b, c, expand, verbose) -> Result.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void three_path_two_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/")
                .withPath(aString(), aString(), aLong())
                .withQuery(aString("expand"), aBoolean("verbose"))
                .toValue((a, b, c, expand, verbose) -> new TestResponse("Found"))
                .asJson();

            assertThat(route).isNotNull();
        }

        // 4 path + 1 query
        @Test
        void four_path_one_query() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/d/")
                .withPath(aString(), aString(), aString(), aLong())
                .withQuery(aString("expand"))
                .to((a, b, c, d, expand) -> Promise.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void four_path_one_query_toResult() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/d/")
                .withPath(aString(), aString(), aString(), aLong())
                .withQuery(aString("expand"))
                .toResult((a, b, c, d, expand) -> Result.success(new TestResponse("Found")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void four_path_one_query_toValue() {
            Route<TestResponse> route = Route.<TestResponse>get("/a/b/c/d/")
                .withPath(aString(), aString(), aString(), aLong())
                .withQuery(aString("expand"))
                .toValue((a, b, c, d, expand) -> new TestResponse("Found"))
                .asJson();

            assertThat(route).isNotNull();
        }
    }

    // ===================================================================================
    // Path + Query + Body Combinations
    // ===================================================================================
    @Nested
    class PathPlusQueryPlusBodyBuilders {
        // 1 path + 1 query + body
        @Test
        void one_path_one_query_body() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/")
                .withPath(aLong())
                .withQuery(aString("notify"))
                .withBody(TestRequest.class)
                .to((id, notify, body) -> Promise.success(new TestResponse("Complete")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_one_query_body_toResult() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/")
                .withPath(aLong())
                .withQuery(aString("notify"))
                .withBody(TestRequest.class)
                .toResult((id, notify, body) -> Result.success(new TestResponse("Complete")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_one_query_body_toJson() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/")
                .withPath(aLong())
                .withQuery(aString("notify"))
                .withBody(TestRequest.class)
                .toJson((id, notify, body) -> Promise.success(new TestResponse("Complete")));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }

        // 1 path + 2 query + body
        @Test
        void one_path_two_query_body() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/")
                .withPath(aLong())
                .withQuery(aString("notify"), aBoolean("force"))
                .withBody(TestRequest.class)
                .to((id, notify, force, body) -> Promise.success(new TestResponse("Complete")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_two_query_body_toResult() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/")
                .withPath(aLong())
                .withQuery(aString("notify"), aBoolean("force"))
                .withBody(TestRequest.class)
                .toResult((id, notify, force, body) -> Result.success(new TestResponse("Complete")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void one_path_two_query_body_toJson() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/")
                .withPath(aLong())
                .withQuery(aString("notify"), aBoolean("force"))
                .withBody(TestRequest.class)
                .toJson((id, notify, force, body) -> Promise.success(new TestResponse("Complete")));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }

        // 2 path + 1 query + body
        @Test
        void two_path_one_query_body() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/orders/")
                .withPath(aLong(), aLong())
                .withQuery(aString("notify"))
                .withBody(TestRequest.class)
                .to((userId, orderId, notify, body) -> Promise.success(new TestResponse("Complete")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_path_one_query_body_toResult() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/orders/")
                .withPath(aLong(), aLong())
                .withQuery(aString("notify"))
                .withBody(TestRequest.class)
                .toResult((userId, orderId, notify, body) -> Result.success(new TestResponse("Complete")))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void two_path_one_query_body_toJson() {
            Route<TestResponse> route = Route.<TestResponse>put("/users/orders/")
                .withPath(aLong(), aLong())
                .withQuery(aString("notify"))
                .withBody(TestRequest.class)
                .toJson((userId, orderId, notify, body) -> Promise.success(new TestResponse("Complete")));

            assertThat(route.contentType()).isEqualTo(CommonContentTypes.APPLICATION_JSON);
        }
    }

    // ===================================================================================
    // HTTP Method Variants
    // ===================================================================================
    @Nested
    class HttpMethodVariants {
        @Test
        void options_method() {
            Route<TestResponse> route = Route.<TestResponse>options("/resource")
                .to(_ -> Promise.success(new TestResponse("Options")))
                .asJson();

            assertThat(route.method()).isEqualTo(OPTIONS);
        }

        @Test
        void head_method() {
            Route<TestResponse> route = Route.<TestResponse>head("/resource")
                .to(_ -> Promise.success(new TestResponse("Head")))
                .asJson();

            assertThat(route.method()).isEqualTo(HEAD);
        }

        @Test
        void patch_method() {
            Route<TestResponse> route = Route.<TestResponse>patch("/resource")
                .withPath(aLong())
                .withBody(TestRequest.class)
                .to((id, body) -> Promise.success(new TestResponse("Patched")))
                .asJson();

            assertThat(route.method()).isEqualTo(PATCH);
        }

        @Test
        void delete_method() {
            Route<TestResponse> route = Route.<TestResponse>delete("/resource/")
                .withPath(aLong())
                .to(id -> Promise.success(new TestResponse("Deleted")))
                .asJson();

            assertThat(route.method()).isEqualTo(DELETE);
        }

        @Test
        void trace_method() {
            Route<TestResponse> route = Route.<TestResponse>trace("/resource")
                .to(_ -> Promise.success(new TestResponse("Trace")))
                .asJson();

            assertThat(route.method()).isEqualTo(TRACE);
        }

        @Test
        void connect_method() {
            Route<TestResponse> route = Route.<TestResponse>connect("/resource")
                .to(_ -> Promise.success(new TestResponse("Connect")))
                .asJson();

            assertThat(route.method()).isEqualTo(CONNECT);
        }

        @Test
        void custom_method() {
            Route<TestResponse> route = Route.<TestResponse>method(POST, "/custom")
                .to(_ -> Promise.success(new TestResponse("Custom")))
                .asJson();

            assertThat(route.method()).isEqualTo(POST);
        }
    }

    // ===================================================================================
    // Subroutes and Path Prefix
    // ===================================================================================
    @Nested
    class SubroutesAndPrefix {
        @Test
        void route_withPrefix() {
            Route<TestResponse> route = Route.<TestResponse>get("/users")
                .to(_ -> Promise.success(new TestResponse("Users")))
                .asJson();

            RouteSource prefixed = route.withPrefix("/api/v1");

            var routes = prefixed.routes().toList();
            assertThat(routes).hasSize(1);
            assertThat(routes.getFirst().path()).isEqualTo("/api/v1/users/");
        }

        @Test
        void subroutes_serve() {
            Route<TestResponse> route1 = Route.<TestResponse>get("/users")
                .to(_ -> Promise.success(new TestResponse("Users")))
                .asJson();

            Route<TestResponse> route2 = Route.<TestResponse>get("/orders")
                .to(_ -> Promise.success(new TestResponse("Orders")))
                .asJson();

            RouteSource grouped = Route.in("/api").serve(route1, route2);

            var routes = grouped.routes().toList();
            assertThat(routes).hasSize(2);
            assertThat(routes.get(0).path()).isEqualTo("/api/users/");
            assertThat(routes.get(1).path()).isEqualTo("/api/orders/");
        }
    }

    // ===================================================================================
    // Path Parameter Types
    // ===================================================================================
    @Nested
    class PathParameterTypes {
        @Test
        void string_path_param() {
            Route<TestResponse> route = Route.<TestResponse>get("/users/")
                .withPath(aString())
                .to(name -> Promise.success(new TestResponse("User: " + name)))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void integer_path_param() {
            Route<TestResponse> route = Route.<TestResponse>get("/items/")
                .withPath(aInteger())
                .to(id -> Promise.success(new TestResponse("Item: " + id)))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void double_path_param() {
            Route<TestResponse> route = Route.<TestResponse>get("/prices/")
                .withPath(aDouble())
                .to(price -> Promise.success(new TestResponse("Price: " + price)))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void boolean_path_param() {
            Route<TestResponse> route = Route.<TestResponse>get("/flags/")
                .withPath(aBoolean())
                .to(flag -> Promise.success(new TestResponse("Flag: " + flag)))
                .asJson();

            assertThat(route).isNotNull();
        }
    }

    // ===================================================================================
    // Query Parameter Types
    // ===================================================================================
    @Nested
    class QueryParameterTypes {
        @Test
        void integer_query_param() {
            Route<TestResponse> route = Route.<TestResponse>get("/items")
                .withQuery(aInteger("page"))
                .to(page -> Promise.success(new TestResponse("Page: " + page.or(1))))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void long_query_param() {
            Route<TestResponse> route = Route.<TestResponse>get("/items")
                .withQuery(aLong("cursor"))
                .to(cursor -> Promise.success(new TestResponse("Cursor: " + cursor.or(0L))))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void boolean_query_param() {
            Route<TestResponse> route = Route.<TestResponse>get("/items")
                .withQuery(aBoolean("active"))
                .to(active -> Promise.success(new TestResponse("Active: " + active.or(true))))
                .asJson();

            assertThat(route).isNotNull();
        }

        @Test
        void double_query_param() {
            Route<TestResponse> route = Route.<TestResponse>get("/items")
                .withQuery(aDouble("minPrice"))
                .to(minPrice -> Promise.success(new TestResponse("Min: " + minPrice.or(0.0))))
                .asJson();

            assertThat(route).isNotNull();
        }
    }

    // ===================================================================================
    // Route toString
    // ===================================================================================
    @Nested
    class RouteToString {
        @Test
        void route_toString_contains_method_and_path() {
            Route<TestResponse> route = Route.<TestResponse>get("/users")
                .to(_ -> Promise.success(new TestResponse("Users")))
                .asJson();

            String str = route.toString();
            assertThat(str).contains("GET");
            assertThat(str).contains("/users/");
            assertThat(str).contains("APPLICATION_JSON");
        }
    }
}
