package org.pragmatica.http.server.routing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.http.protocol.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class RequestRouterTest {
    private final RequestRouter table = RequestRouter.with(
        Route.get("/one").toText(() -> "one"),
        Route.get("/one1").toText(() -> "one1"),
        Route.get("/one2").toText(() -> "one2"),
        Route.get("/on").toText(() -> "on"),
        Route.get("/o").toText(() -> "o")
    );

    @Test
    void existingRouteIsReturnedProperly() {
        checkSingle("/one");
        checkSingle("/one1");
        checkSingle("/one2");
        checkSingle("/on");
        checkSingle("/o");
    }

    @Test
    void notExistingRouteIsReturnedProperly() {
        assertTrue(table.findRoute(HttpMethod.GET, "/").isEmpty());
        assertTrue(table.findRoute(HttpMethod.GET, "/oo").isEmpty());
        assertTrue(table.findRoute(HttpMethod.GET, "/om").isEmpty());
        assertTrue(table.findRoute(HttpMethod.GET, "/one3").isEmpty());
    }

    private void checkSingle(String path) {
        table.findRoute(HttpMethod.GET, path)
            .onEmpty(Assertions::fail)
            .onPresent(route -> route.handler().handle(null)
                .onSuccess(value -> assertEquals(path, STR."/\{value}"))
                .onFailure(_ -> fail()));
    }
}