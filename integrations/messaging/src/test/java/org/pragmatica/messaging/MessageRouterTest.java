/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.messaging;

import org.junit.jupiter.api.Test;
import org.pragmatica.messaging.Message.Local;
import org.pragmatica.messaging.MessageRouterTest.LocalMessages.LocalMessage1;
import org.pragmatica.messaging.MessageRouterTest.LocalMessages.LocalMessage2;
import org.pragmatica.messaging.MessageRouterTest.LocalMessages.LocalMessage3;
import org.pragmatica.messaging.MessageRouterTest.WiredMessages.WiredMessage1;
import org.pragmatica.messaging.MessageRouterTest.WiredMessages.WiredMessage2;
import org.pragmatica.messaging.MessageRouterTest.WiredMessages.WiredMessage3;
import org.pragmatica.messaging.MessageRouterTest.WiredMessages.WiredMessage4;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.messaging.MessageRouter.Entry.SealedBuilder.from;
import static org.pragmatica.messaging.MessageRouter.Entry.route;

class MessageRouterTest {

    sealed interface LocalMessages extends Local {
        record LocalMessage1() implements LocalMessages {}
        record LocalMessage2() implements LocalMessages {}
        record LocalMessage3() implements LocalMessages {}
    }

    sealed interface WiredMessages extends Message.Wired {
        record WiredMessage1() implements WiredMessages {}
        record WiredMessage2() implements WiredMessages {}
        record WiredMessage3() implements WiredMessages {}
        record WiredMessage4() implements WiredMessages {}
    }

    record LocalHandler() {
        @MessageReceiver
        public void handle1(LocalMessage1 message) {}

        @MessageReceiver
        public void handle2(LocalMessage2 message) {}

        @MessageReceiver
        public void handle3(LocalMessage3 message) {}
    }

    record WiredHandler() {
        @MessageReceiver
        public void handle1(WiredMessage1 message) {}

        @MessageReceiver
        public void handle2(WiredMessages.WiredMessage2 message) {}

        @MessageReceiver
        public void handle3(WiredMessages.WiredMessage3 message) {}

        @MessageReceiver
        public void handle4(WiredMessages.WiredMessage4 message) {}
    }

    @Test
    void sealed_builder_validates_all_subtypes_covered() {
        var localHandler = new LocalHandler();
        var wiredHandler = new WiredHandler();

        var routes = from(Message.class)
            .route(
                from(Local.class)
                    .route(from(LocalMessages.class)
                               .route(route(LocalMessage1.class, localHandler::handle1),
                                      route(LocalMessage2.class, localHandler::handle2),
                                      route(LocalMessage3.class, localHandler::handle3))),
                from(Message.Wired.class)
                    .route(from(WiredMessages.class)
                               .route(route(WiredMessage1.class, wiredHandler::handle1),
                                      route(WiredMessage2.class, wiredHandler::handle2),
                                      route(WiredMessage3.class, wiredHandler::handle3),
                                      route(WiredMessage4.class, wiredHandler::handle4)))
            );

        assertThat(routes.validate()).isEmpty();
        assertThat(routes.asRouter().isSuccess()).isTrue();
    }

    @Test
    void mutable_router_routes_messages() {
        var counter = new AtomicInteger(0);
        var router = MessageRouter.mutable();
        router.addRoute(LocalMessage1.class, msg -> counter.incrementAndGet());

        router.route(new LocalMessage1());
        router.route(new LocalMessage1());

        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void mutable_router_supports_multiple_handlers() {
        var counter = new AtomicInteger(0);
        var router = MessageRouter.mutable();
        router.addRoute(LocalMessage1.class, msg -> counter.incrementAndGet());
        router.addRoute(LocalMessage1.class, msg -> counter.addAndGet(10));

        router.route(new LocalMessage1());

        assertThat(counter.get()).isEqualTo(11);
    }

    @Test
    void immutable_router_routes_messages() {
        var counter = new AtomicInteger(0);

        var routes = from(LocalMessages.class)
            .route(
                route(LocalMessage1.class, msg -> counter.incrementAndGet()),
                route(LocalMessage2.class, msg -> {}),
                route(LocalMessage3.class, msg -> {})
            );

        var routerResult = routes.asRouter();
        assertThat(routerResult.isSuccess()).isTrue();

        routerResult.onSuccess(router -> {
            router.route(new LocalMessage1());
            router.route(new LocalMessage1());
        });

        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void validation_fails_for_missing_subtypes() {
        var routes = from(LocalMessages.class)
            .route(
                route(LocalMessage1.class, msg -> {}),
                route(LocalMessage2.class, msg -> {})
                // Missing LocalMessage3
            );

        assertThat(routes.validate()).isNotEmpty();
        assertThat(routes.validate()).anyMatch(c -> c.getSimpleName().equals("LocalMessage3"));
    }
}
