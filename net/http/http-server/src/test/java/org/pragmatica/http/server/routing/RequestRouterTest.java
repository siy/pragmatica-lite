package org.pragmatica.http.server.routing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.http.protocol.HttpMethod;

import static org.junit.jupiter.api.Assertions.*;

class RequestRouterTest {
    private final RequestRouter table = RequestRouter.with(
        Route.handleGet("/one").withText(() -> "one"),
        Route.handleGet("/one1").withText(() -> "one1"),
        Route.handleGet("/one2").withText(() -> "one2"),
        Route.handleGet("/on").withText(() -> "on"),
        Route.handleGet("/o").withText(() -> "o")
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
                .onSuccess(value -> assertEquals(path, "/" + value))
                .onFailure(_ -> fail()));
    }
}