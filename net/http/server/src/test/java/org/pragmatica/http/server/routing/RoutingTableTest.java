package org.pragmatica.http.server.routing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.http.protocol.HttpMethod;
import org.pragmatica.lang.utils.Causes;

import static org.junit.jupiter.api.Assertions.*;

class RequestRouterTest {
    private final RequestRouter table = RequestRouter.with(
        Route.get("/one").textWith(() -> Causes.cause("one").result()),
        Route.get("/one1").textWith(() -> Causes.cause("one1").result()),
        Route.get("/one2").textWith(() -> Causes.cause("one2").result()),
        Route.get("/on").textWith(() -> Causes.cause("on").result()),
        Route.get("/o").textWith(() -> Causes.cause("o").result())
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
                .onFailure(cause -> assertEquals(path, "/" + cause.message()))
                .onSuccess(_ -> fail()));
    }
}