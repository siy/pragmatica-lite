package org.pragmatica.http.server.routing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.http.protocol.HttpMethod;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.http.server.routing.Route.whenGet;

class RequestRouterTest {
    private final RequestRouter table = RequestRouter.with(
        whenGet("/one").returnText(() -> "one"),
        whenGet("/one1").returnText(() -> "one1"),
        whenGet("/one2").returnText(() -> "one2"),
        whenGet("/on").returnText(() -> "on"),
        whenGet("/o").returnText(() -> "o")
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