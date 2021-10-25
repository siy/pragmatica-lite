package org.pfj.http.server.routing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pfj.http.server.routing.RoutingTable;
import org.pfj.lang.Causes;

import io.netty.handler.codec.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.pfj.http.server.routing.Route.from;

class RoutingTableTest {
    private RoutingTable table = RoutingTable.with(
        from("/one").get().text().from(() -> Causes.cause("one").result()),
        from("/one1").get().text().from(() -> Causes.cause("one1").result()),
        from("/one2").get().text().from(() -> Causes.cause("one2").result()),
        from("/on").get().text().from(() -> Causes.cause("on").result()),
        from("/o").get().text().from(() -> Causes.cause("o").result())
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