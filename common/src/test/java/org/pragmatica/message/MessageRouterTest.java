package org.pragmatica.message;

import org.junit.jupiter.api.Test;
import org.pragmatica.message.Message.Local;
import org.pragmatica.message.MessageRouterTest.LocalMessages.LocalMessage1;
import org.pragmatica.message.MessageRouterTest.LocalMessages.LocalMessage2;
import org.pragmatica.message.MessageRouterTest.LocalMessages.LocalMessage3;
import org.pragmatica.message.MessageRouterTest.WiredMessages.WiredMessage1;
import org.pragmatica.message.MessageRouterTest.WiredMessages.WiredMessage2;
import org.pragmatica.message.MessageRouterTest.WiredMessages.WiredMessage3;
import org.pragmatica.message.MessageRouterTest.WiredMessages.WiredMessage4;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.pragmatica.message.MessageRouter.Entry.SealedBuilder.from;
import static org.pragmatica.message.MessageRouter.Entry.route;

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
        public void handle1(LocalMessage1 message) {
        }

        @MessageReceiver
        public void handle2(LocalMessage2 message) {
        }

        @MessageReceiver
        public void handle3(LocalMessage3 message) {
        }
    }

    record WiredHandler() {
        @MessageReceiver
        public void handle1(WiredMessage1 message) {
        }

        @MessageReceiver
        public void handle2(WiredMessages.WiredMessage2 message) {
        }

        @MessageReceiver
        public void handle3(WiredMessages.WiredMessage3 message) {
        }

        @MessageReceiver
        public void handle4(WiredMessages.WiredMessage4 message) {
        }
    }


    @SuppressWarnings("unchecked")
    @Test
    void assemblingExample() {
        var localHandler = new LocalHandler();
        var wiredHandler = new WiredHandler();

        var routes = from(Message.class)
                .route(from(Local.class)
                               .route(from(LocalMessages.class)
                                              .route(route(LocalMessage1.class, localHandler::handle1),
                                                     route(LocalMessage2.class, localHandler::handle2),
                                                     route(LocalMessage3.class, localHandler::handle3))),
                       from(Message.Wired.class)
                               .route(from(WiredMessages.class)
                                              .route(route(WiredMessage1.class, wiredHandler::handle1),
                                                     route(WiredMessage2.class, wiredHandler::handle2),
                                                     route(WiredMessage3.class, wiredHandler::handle3),
                                                     route(WiredMessage4.class, wiredHandler::handle4))));

        assertTrue(routes.validate().isEmpty());
        assertTrue(routes.asRouter().isSuccess());
    }
}