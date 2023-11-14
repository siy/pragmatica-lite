package org.pfj.http.server.routing;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.utils.Causes;

import static org.junit.jupiter.api.Assertions.*;
import static org.pfj.http.server.routing.Route.from;

class RoutingTableTest {
    private final RoutingTable table = RoutingTable.with(
        from("/one").get().text().with(() -> Causes.cause("one").result()),
        from("/one1").get().text().with(() -> Causes.cause("one1").result()),
        from("/one2").get().text().with(() -> Causes.cause("one2").result()),
        from("/on").get().text().with(() -> Causes.cause("on").result()),
        from("/o").get().text().with(() -> Causes.cause("o").result())
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