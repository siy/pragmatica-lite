package org.pfj.http.server;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pfj.lang.Causes;

import static org.junit.jupiter.api.Assertions.*;
import static org.pfj.http.server.Route.getText;
import static org.pfj.http.server.Route.postText;

class EndpointTableTest {
    private EndpointTable table = EndpointTable.with(
        getText("/one", () -> Causes.cause("one").result()),
        getText("/one1", () -> Causes.cause("one1").result()),
        getText("/one2", () -> Causes.cause("one2").result()),
        getText("/on", () -> Causes.cause("on").result()),
        getText("/o", () -> Causes.cause("o").result()),
        pos tText("/one", __ -> Causes.cause("one").result()),
        postText("/one1", __ -> Causes.cause("one1").result()),
        postText("/one2", __ -> Causes.cause("one2").result()),
        postText("/on", __ -> Causes.cause("on").result()),
        postText("/o", __ -> Causes.cause("o").result())
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
            .whenEmpty(Assertions::fail)
            .whenPresent(route -> route.handler().handle(null)
                .onFailure(cause -> assertEquals(path, "/" + cause.message()))
                .onSuccess(__ -> fail()));
    }
}