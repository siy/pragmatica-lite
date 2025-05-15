package org.pragmatica.message;

import org.pragmatica.lang.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/// **NOTE**: This implementation assumes that the instance is configured and then used without
/// changes.
public interface MessageRouter {
    <T extends Message> void route(T message);

    <T extends Message> MessageRouter addRoute(Class<T> messageType, Consumer<T> receiver);

    void validate(List<Class<? extends Message>> messageTypes);

    static MessageRouter messageRouter() {
        @SuppressWarnings("unchecked")
        record messageRouter<T extends Message>(Map<Class<T>, List<Consumer<T>>> routingTable) implements
                MessageRouter {

            private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);

            @Override
            public <R extends Message> void route(R message) {
                Option.option(routingTable.get(message.getClass()))
                        .onPresent(list -> list.forEach(fn -> fn.accept((T) message)))
                        .onEmpty(() -> log.warn("No route for message type: {}", message.getClass().getSimpleName()));
            }

            @Override
            public <R extends Message> MessageRouter addRoute(Class<R> messageType, Consumer<R> receiver) {
                routingTable.computeIfAbsent((Class<T>) messageType, _ -> new ArrayList<>())
                            .add((Consumer<T>) receiver);

                return this;
            }

            @Override
            public void validate(List<Class<? extends Message>> messageTypes) {
                var missing = messageTypes.stream()
                                          .filter(messageType -> !routingTable.containsKey(messageType))
                                          .map(Class::getSimpleName)
                                          .toList();

                if (!missing.isEmpty()) {
                    throw new IllegalArgumentException("Missing message types: " + String.join(", ", missing));
                }
            }
        }

        return new messageRouter<>(new HashMap<>());
    }
}
