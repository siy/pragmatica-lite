package org.pragmatica.http.routing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Promise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.http.routing.HttpMethod.GET;
import static org.pragmatica.http.routing.PathParameter.aLong;
import static org.pragmatica.http.routing.PathParameter.spacer;
import static org.pragmatica.http.routing.Route.get;

class SpacerRouteTest {

    record TestResponse(String result) {}

    @Nested
    class SpacerParsing {
        @Test
        void spacer_succeeds_forExactMatch() {
            var spacerParam = spacer("edit");

            spacerParam.parse("edit")
                       .onFailure(_ -> org.junit.jupiter.api.Assertions.fail())
                       .onSuccess(value -> assertThat(value).isEqualTo("edit"));
        }

        @Test
        void spacer_fails_forMismatch() {
            var spacerParam = spacer("edit");

            spacerParam.parse("delete")
                       .onSuccess(_ -> org.junit.jupiter.api.Assertions.fail())
                       .onFailure(cause -> {
                           assertThat(cause).isInstanceOf(ParameterError.PathMismatch.class);
                           assertThat(cause.message()).contains("edit").contains("delete");
                       });
        }

        @Test
        void spacer_fails_forEmptyValue() {
            var spacerParam = spacer("edit");

            spacerParam.parse("")
                       .onSuccess(_ -> org.junit.jupiter.api.Assertions.fail());
        }

        @Test
        void spacer_recordContainsText() {
            var spacerParam = spacer("details");

            assertThat(spacerParam).isInstanceOf(PathParameter.Spacer.class);
            assertThat(((PathParameter.Spacer) spacerParam).text()).isEqualTo("details");
        }
    }

    @Nested
    class RouteSpacersCollection {
        @Test
        void route_withSpacer_hasSingleSpacerInList() {
            Route<TestResponse> route = Route.<TestResponse>get("/api/users/")
                .withPath(aLong(), spacer("edit"))
                .to((id, _) -> Promise.success(new TestResponse("Edit user " + id)))
                .asJson();

            assertThat(route.spacers()).containsExactly("edit");
        }

        @Test
        void route_withMultipleSpacers_hasAllSpacersInList() {
            Route<TestResponse> route = Route.<TestResponse>get("/api/")
                .withPath(spacer("users"), aLong(), spacer("profile"))
                .to((_, id, _) -> Promise.success(new TestResponse("Profile " + id)))
                .asJson();

            assertThat(route.spacers()).containsExactly("users", "profile");
        }

        @Test
        void route_withoutSpacers_hasEmptySpacersList() {
            Route<TestResponse> route = Route.<TestResponse>get("/api/users/")
                .withPath(aLong())
                .to(id -> Promise.success(new TestResponse("User " + id)))
                .asJson();

            assertThat(route.spacers()).isEmpty();
        }

        @Test
        void route_withNoPathParams_hasEmptySpacersList() {
            Route<TestResponse> route = Route.<TestResponse>get("/api/health")
                .to(_ -> Promise.success(new TestResponse("OK")))
                .asJson();

            assertThat(route.spacers()).isEmpty();
        }

        @Test
        void route_toString_includesSpacers() {
            Route<TestResponse> route = Route.<TestResponse>get("/api/users/")
                .withPath(aLong(), spacer("edit"))
                .to((id, _) -> Promise.success(new TestResponse("Edit " + id)))
                .asJson();

            assertThat(route.toString()).contains("edit");
        }
    }

    @Nested
    class RequestRouterSpacerSelection {
        @Test
        void router_findsRouteByMatchingSpacer() {
            Route<TestResponse> editRoute = Route.<TestResponse>get("/api/users/")
                .withPath(aLong(), spacer("edit"))
                .to((id, _) -> Promise.success(new TestResponse("Edit " + id)))
                .asJson();

            Route<TestResponse> deleteRoute = Route.<TestResponse>get("/api/users/")
                .withPath(aLong(), spacer("delete"))
                .to((id, _) -> Promise.success(new TestResponse("Delete " + id)))
                .asJson();

            var router = RequestRouter.with(editRoute, deleteRoute);

            router.findRoute(GET, "/api/users/123/edit")
                  .onEmpty(org.junit.jupiter.api.Assertions::fail)
                  .onPresent(route -> assertThat(route.spacers()).containsExactly("edit"));

            router.findRoute(GET, "/api/users/456/delete")
                  .onEmpty(org.junit.jupiter.api.Assertions::fail)
                  .onPresent(route -> assertThat(route.spacers()).containsExactly("delete"));
        }

        @Test
        void router_fallbackToRouteWithoutSpacers() {
            Route<TestResponse> editRoute = Route.<TestResponse>get("/api/users/")
                .withPath(aLong(), spacer("edit"))
                .to((id, _) -> Promise.success(new TestResponse("Edit " + id)))
                .asJson();

            Route<TestResponse> fallbackRoute = Route.<TestResponse>get("/api/users/")
                .withPath(aLong())
                .to(id -> Promise.success(new TestResponse("View " + id)))
                .asJson();

            var router = RequestRouter.with(editRoute, fallbackRoute);

            // Request with edit spacer matches edit route
            router.findRoute(GET, "/api/users/123/edit")
                  .onEmpty(org.junit.jupiter.api.Assertions::fail)
                  .onPresent(route -> assertThat(route.spacers()).containsExactly("edit"));

            // Request without spacer falls back to generic route
            router.findRoute(GET, "/api/users/123")
                  .onEmpty(org.junit.jupiter.api.Assertions::fail)
                  .onPresent(route -> assertThat(route.spacers()).isEmpty());
        }

        @Test
        void router_singleRouteReturnsWithoutSpacerMatching() {
            Route<TestResponse> route = Route.<TestResponse>get("/api/items/")
                .withPath(aLong())
                .to(id -> Promise.success(new TestResponse("Item " + id)))
                .asJson();

            var router = RequestRouter.with(route);

            router.findRoute(GET, "/api/items/42")
                  .onEmpty(org.junit.jupiter.api.Assertions::fail)
                  .onPresent(found -> assertThat(found.path()).isEqualTo("/api/items/"));
        }

        @Test
        void router_threeRoutesWithDifferentSpacers() {
            Route<TestResponse> viewRoute = Route.<TestResponse>get("/api/orders/")
                .withPath(aLong(), spacer("view"))
                .to((id, _) -> Promise.success(new TestResponse("View " + id)))
                .asJson();

            Route<TestResponse> cancelRoute = Route.<TestResponse>get("/api/orders/")
                .withPath(aLong(), spacer("cancel"))
                .to((id, _) -> Promise.success(new TestResponse("Cancel " + id)))
                .asJson();

            Route<TestResponse> refundRoute = Route.<TestResponse>get("/api/orders/")
                .withPath(aLong(), spacer("refund"))
                .to((id, _) -> Promise.success(new TestResponse("Refund " + id)))
                .asJson();

            var router = RequestRouter.with(viewRoute, cancelRoute, refundRoute);

            router.findRoute(GET, "/api/orders/1/view")
                  .onEmpty(org.junit.jupiter.api.Assertions::fail)
                  .onPresent(route -> assertThat(route.spacers()).containsExactly("view"));

            router.findRoute(GET, "/api/orders/2/cancel")
                  .onEmpty(org.junit.jupiter.api.Assertions::fail)
                  .onPresent(route -> assertThat(route.spacers()).containsExactly("cancel"));

            router.findRoute(GET, "/api/orders/3/refund")
                  .onEmpty(org.junit.jupiter.api.Assertions::fail)
                  .onPresent(route -> assertThat(route.spacers()).containsExactly("refund"));
        }

        @Test
        void router_noMatchReturnsEmpty() {
            Route<TestResponse> route = Route.<TestResponse>get("/api/users/")
                .withPath(aLong())
                .to(id -> Promise.success(new TestResponse("User " + id)))
                .asJson();

            var router = RequestRouter.with(route);

            router.findRoute(GET, "/api/orders/123")
                  .onPresent(_ -> org.junit.jupiter.api.Assertions.fail());
        }
    }
}
