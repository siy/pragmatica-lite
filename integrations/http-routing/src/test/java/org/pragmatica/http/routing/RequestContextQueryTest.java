package org.pragmatica.http.routing;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.pragmatica.http.routing.QueryParameter.*;

class RequestContextQueryTest {

    /**
     * Test implementation of RequestContext for testing query parameter matching.
     */
    static class TestRequestContext implements RequestContext {
        private final Map<String, List<String>> queryParams;

        TestRequestContext(Map<String, List<String>> queryParams) {
            this.queryParams = queryParams;
        }

        @Override
        public Route<?> route() {
            return null;
        }

        @Override
        public String requestId() {
            return "test-request-id";
        }

        @Override
        public ByteBuf body() {
            return Unpooled.EMPTY_BUFFER;
        }

        @Override
        public String bodyAsString() {
            return "";
        }

        @Override
        public <T> Result<T> fromJson(TypeToken<T> literal) {
            return Result.failure(HttpStatus.BAD_REQUEST.with("Not implemented"));
        }

        @Override
        public List<String> pathParams() {
            return List.of();
        }

        @Override
        public Map<String, List<String>> queryParams() {
            return queryParams;
        }

        @Override
        public Map<String, String> requestHeaders() {
            return Map.of();
        }

        @Override
        public HttpHeaders responseHeaders() {
            return new DefaultHttpHeaders();
        }
    }

    @Nested
    class SingleQueryParameter {
        @Test
        void matchQuery_succeeds_forPresentParameter() {
            var ctx = new TestRequestContext(Map.of("name", List.of("John")));

            ctx.matchQuery(aString("name"))
               .map(opt -> opt)
               .onFailure(_ -> fail())
               .onSuccess(opt -> opt.onPresent(value -> assertEquals("John", value))
                                    .onEmpty(() -> fail()));
        }

        @Test
        void matchQuery_returnsNone_forMissingParameter() {
            var ctx = new TestRequestContext(Map.of());

            ctx.matchQuery(aString("name"))
               .map(opt -> opt)
               .onFailure(_ -> fail())
               .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void matchQuery_fails_forInvalidValue() {
            var ctx = new TestRequestContext(Map.of("count", List.of("not-a-number")));

            ctx.matchQuery(aInteger("count"))
               .map(opt -> opt)
               .onSuccess(_ -> fail());
        }

        @Test
        void matchQuery_succeeds_forValidInteger() {
            var ctx = new TestRequestContext(Map.of("page", List.of("42")));

            ctx.matchQuery(aInteger("page"))
               .map(opt -> opt)
               .onFailure(_ -> fail())
               .onSuccess(opt -> opt.onPresent(value -> assertEquals(42, value))
                                    .onEmpty(() -> fail()));
        }
    }

    @Nested
    class TwoQueryParameters {
        @Test
        void matchQuery_succeeds_forBothPresent() {
            var ctx = new TestRequestContext(Map.of(
                "query", List.of("search term"),
                "page", List.of("5")
            ));

            ctx.matchQuery(aString("query"), aInteger("page"))
               .map((q, p) -> new Object() {
                   final Option<String> query = q;
                   final Option<Integer> page = p;
               })
               .onFailure(_ -> fail())
               .onSuccess(result -> {
                   result.query.onPresent(v -> assertEquals("search term", v))
                               .onEmpty(() -> fail());
                   result.page.onPresent(v -> assertEquals(5, v))
                              .onEmpty(() -> fail());
               });
        }

        @Test
        void matchQuery_succeeds_forOnePresent() {
            var ctx = new TestRequestContext(Map.of("query", List.of("search")));

            ctx.matchQuery(aString("query"), aInteger("page"))
               .map((q, p) -> new Object() {
                   final Option<String> query = q;
                   final Option<Integer> page = p;
               })
               .onFailure(_ -> fail())
               .onSuccess(result -> {
                   result.query.onPresent(v -> assertEquals("search", v))
                               .onEmpty(() -> fail());
                   result.page.onPresent(_ -> fail());
               });
        }

        @Test
        void matchQuery_succeeds_forNonePresent() {
            var ctx = new TestRequestContext(Map.of());

            ctx.matchQuery(aString("query"), aInteger("page"))
               .map((q, p) -> new Object() {
                   final Option<String> query = q;
                   final Option<Integer> page = p;
               })
               .onFailure(_ -> fail())
               .onSuccess(result -> {
                   result.query.onPresent(_ -> fail());
                   result.page.onPresent(_ -> fail());
               });
        }

        @Test
        void matchQuery_fails_forInvalidFirstParam() {
            var ctx = new TestRequestContext(Map.of(
                "count", List.of("invalid"),
                "page", List.of("5")
            ));

            ctx.matchQuery(aInteger("count"), aInteger("page"))
               .map((c, p) -> c)
               .onSuccess(_ -> fail());
        }

        @Test
        void matchQuery_fails_forInvalidSecondParam() {
            var ctx = new TestRequestContext(Map.of(
                "query", List.of("valid"),
                "page", List.of("invalid")
            ));

            ctx.matchQuery(aString("query"), aInteger("page"))
               .map((q, p) -> p)
               .onSuccess(_ -> fail());
        }
    }

    @Nested
    class ThreeQueryParameters {
        @Test
        void matchQuery_succeeds_forAllPresent() {
            var ctx = new TestRequestContext(Map.of(
                "q", List.of("search"),
                "page", List.of("1"),
                "size", List.of("20")
            ));

            ctx.matchQuery(aString("q"), aInteger("page"), aInteger("size"))
               .map((q, p, s) -> new Object() {
                   final Option<String> query = q;
                   final Option<Integer> page = p;
                   final Option<Integer> size = s;
               })
               .onFailure(_ -> fail())
               .onSuccess(result -> {
                   result.query.onPresent(v -> assertEquals("search", v))
                               .onEmpty(() -> fail());
                   result.page.onPresent(v -> assertEquals(1, v))
                              .onEmpty(() -> fail());
                   result.size.onPresent(v -> assertEquals(20, v))
                              .onEmpty(() -> fail());
               });
        }

        @Test
        void matchQuery_succeeds_forPartialPresent() {
            var ctx = new TestRequestContext(Map.of(
                "q", List.of("search"),
                "size", List.of("50")
            ));

            ctx.matchQuery(aString("q"), aInteger("page"), aInteger("size"))
               .map((q, p, s) -> new Object() {
                   final Option<String> query = q;
                   final Option<Integer> page = p;
                   final Option<Integer> size = s;
               })
               .onFailure(_ -> fail())
               .onSuccess(result -> {
                   result.query.onPresent(v -> assertEquals("search", v))
                               .onEmpty(() -> fail());
                   result.page.onPresent(_ -> fail());
                   result.size.onPresent(v -> assertEquals(50, v))
                              .onEmpty(() -> fail());
               });
        }

        @Test
        void matchQuery_fails_forInvalidThirdParam() {
            var ctx = new TestRequestContext(Map.of(
                "q", List.of("search"),
                "page", List.of("1"),
                "size", List.of("invalid")
            ));

            ctx.matchQuery(aString("q"), aInteger("page"), aInteger("size"))
               .map((q, p, s) -> s)
               .onSuccess(_ -> fail());
        }
    }

    @Nested
    class FourQueryParameters {
        @Test
        void matchQuery_succeeds_forAllPresent() {
            var ctx = new TestRequestContext(Map.of(
                "q", List.of("search"),
                "page", List.of("1"),
                "size", List.of("20"),
                "asc", List.of("true")
            ));

            ctx.matchQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"))
               .map((q, p, s, a) -> new Object() {
                   final Option<String> query = q;
                   final Option<Integer> page = p;
                   final Option<Integer> size = s;
                   final Option<Boolean> asc = a;
               })
               .onFailure(_ -> fail())
               .onSuccess(result -> {
                   result.query.onPresent(v -> assertEquals("search", v))
                               .onEmpty(() -> fail());
                   result.page.onPresent(v -> assertEquals(1, v))
                              .onEmpty(() -> fail());
                   result.size.onPresent(v -> assertEquals(20, v))
                              .onEmpty(() -> fail());
                   result.asc.onPresent(v -> assertEquals(true, v))
                             .onEmpty(() -> fail());
               });
        }

        @Test
        void matchQuery_fails_forInvalidFourthParam() {
            var ctx = new TestRequestContext(Map.of(
                "q", List.of("search"),
                "page", List.of("1"),
                "size", List.of("20"),
                "asc", List.of("maybe")
            ));

            ctx.matchQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"))
               .map((q, p, s, a) -> a)
               .onSuccess(_ -> fail());
        }
    }

    @Nested
    class FiveQueryParameters {
        @Test
        void matchQuery_succeeds_forAllPresent() {
            var ctx = new TestRequestContext(Map.of(
                "q", List.of("search"),
                "page", List.of("1"),
                "size", List.of("20"),
                "asc", List.of("true"),
                "sort", List.of("name")
            ));

            ctx.matchQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"), aString("sort"))
               .map((q, p, s, a, sort) -> new Object() {
                   final Option<String> query = q;
                   final Option<Integer> page = p;
                   final Option<Integer> size = s;
                   final Option<Boolean> asc = a;
                   final Option<String> sortField = sort;
               })
               .onFailure(_ -> fail())
               .onSuccess(result -> {
                   result.query.onPresent(v -> assertEquals("search", v))
                               .onEmpty(() -> fail());
                   result.page.onPresent(v -> assertEquals(1, v))
                              .onEmpty(() -> fail());
                   result.size.onPresent(v -> assertEquals(20, v))
                              .onEmpty(() -> fail());
                   result.asc.onPresent(v -> assertEquals(true, v))
                             .onEmpty(() -> fail());
                   result.sortField.onPresent(v -> assertEquals("name", v))
                                   .onEmpty(() -> fail());
               });
        }

        @Test
        void matchQuery_succeeds_forNonePresent() {
            var ctx = new TestRequestContext(Map.of());

            ctx.matchQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"), aString("sort"))
               .map((q, p, s, a, sort) -> new Object() {
                   final Option<String> query = q;
                   final Option<Integer> page = p;
                   final Option<Integer> size = s;
                   final Option<Boolean> asc = a;
                   final Option<String> sortField = sort;
               })
               .onFailure(_ -> fail())
               .onSuccess(result -> {
                   result.query.onPresent(_ -> fail());
                   result.page.onPresent(_ -> fail());
                   result.size.onPresent(_ -> fail());
                   result.asc.onPresent(_ -> fail());
                   result.sortField.onPresent(_ -> fail());
               });
        }

        @Test
        void matchQuery_fails_forInvalidFifthParam() {
            var ctx = new TestRequestContext(Map.of(
                "q", List.of("search"),
                "page", List.of("1"),
                "size", List.of("20"),
                "asc", List.of("true"),
                "limit", List.of("invalid")
            ));

            ctx.matchQuery(aString("q"), aInteger("page"), aInteger("size"), aBoolean("asc"), aLong("limit"))
               .map((q, p, s, a, l) -> l)
               .onSuccess(_ -> fail());
        }
    }

    @Nested
    class MixedParameterTypes {
        @Test
        void matchQuery_succeeds_forMixedTypes() {
            var ctx = new TestRequestContext(Map.of(
                "name", List.of("test"),
                "count", List.of("100"),
                "active", List.of("yes"),
                "price", List.of("19.99")
            ));

            ctx.matchQuery(aString("name"), aLong("count"), aBoolean("active"), aDouble("price"))
               .map((n, c, a, p) -> new Object() {
                   final Option<String> name = n;
                   final Option<Long> count = c;
                   final Option<Boolean> active = a;
                   final Option<Double> price = p;
               })
               .onFailure(_ -> fail())
               .onSuccess(result -> {
                   result.name.onPresent(v -> assertEquals("test", v))
                              .onEmpty(() -> fail());
                   result.count.onPresent(v -> assertEquals(100L, v))
                               .onEmpty(() -> fail());
                   result.active.onPresent(v -> assertEquals(true, v))
                                .onEmpty(() -> fail());
                   result.price.onPresent(v -> assertEquals(19.99, v))
                               .onEmpty(() -> fail());
               });
        }
    }

    @Nested
    class DefaultValues {
        @Test
        void matchQuery_allowsDefaultValue_forMissingParam() {
            var ctx = new TestRequestContext(Map.of());

            ctx.matchQuery(aInteger("page"))
               .map(opt -> opt.or(1))
               .onFailure(_ -> fail())
               .onSuccess(value -> assertEquals(1, value));
        }

        @Test
        void matchQuery_usesProvidedValue_whenPresent() {
            var ctx = new TestRequestContext(Map.of("page", List.of("5")));

            ctx.matchQuery(aInteger("page"))
               .map(opt -> opt.or(1))
               .onFailure(_ -> fail())
               .onSuccess(value -> assertEquals(5, value));
        }
    }
}
