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

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.lang.Tuple.tuple;

/// Type-safe message router with support for sealed interface validation.
///
/// This router supports two modes:
///
///   - Mutable - routes can be added dynamically at runtime
///   - Immutable - routes are fixed at construction time with compile-time validation
///
///// **NOTE**: The mutable implementation assumes that the instance is configured and then used
/// without changes.
public sealed interface MessageRouter {
    /// Route a message to its registered handlers.
    ///
    /// @param message the message to route
    /// @param <T>     the message type
    <T extends Message> void route(T message);

    /// Route a message asynchronously.
    ///
    /// @param messageSupplier supplier that creates the message
    /// @param <T>             the message type
    default <T extends Message> void routeAsync(Supplier<T> messageSupplier) {
        Promise.async(() -> route(messageSupplier.get()));
    }

    /// This class is necessary to solve chicken-and-egg problem: many classes which handle messages
    /// need an instance of MessageRouter for construction. Instance of this class is passed to them.
    /// Actual delegate is set when the MessageRouter instance is constructed.
    sealed interface DelegateRouter extends MessageRouter {
        void replaceDelegate(MessageRouter delegate);

        static DelegateRouter delegate() {
            return new DelegateRouterImpl();
        }

        // WARNING: implementation is not thread-safe. It meant to be configured before use.
        final class DelegateRouterImpl implements DelegateRouter {
            private MessageRouter delegate;

            @Override
            public void replaceDelegate(MessageRouter delegate) {
                this.delegate = delegate;
            }

            @Override
            public <T extends Message> void route(T message) {
                delegate.route(message);
            }
        }
    }

    /// Create a new mutable router.
    static MutableRouter mutable() {
        return new MutableRouter.SimpleMutableRouter<>(new ConcurrentHashMap<>());
    }

    /// Mutable router that allows adding routes at runtime.
    sealed interface MutableRouter extends MessageRouter {
        /// Add a route for a message type.
        ///
        /// @param messageType the message class
        /// @param receiver    the handler for messages of this type
        /// @param <T>         the message type
        /// @return this router for chaining
        <T extends Message> MessageRouter addRoute(Class< ? extends T> messageType, Consumer< ? extends T> receiver);

        record SimpleMutableRouter<T extends Message>(ConcurrentMap<Class<T>, List<Consumer<T>>> routingTable) implements MutableRouter {
            private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);

            @Override
            @SuppressWarnings("unchecked")
            public <R extends Message> void route(R message) {
                Option.option(routingTable.get(message.getClass()))
                      .onPresent(list -> list.forEach(fn -> fn.accept((T) message)))
                      .onEmpty(() -> log.warn("No route for message type: {}",
                                              message.getClass()
                                                     .getSimpleName()));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R extends Message> MessageRouter addRoute(Class< ? extends R> messageType,
                                                              Consumer< ? extends R> receiver) {
                routingTable.computeIfAbsent((Class<T>) messageType,
                                             _ -> new CopyOnWriteArrayList<>())
                            .add((Consumer<T>) receiver);
                return this;
            }
        }
    }

    /// Immutable router with fixed routes.
    non-sealed interface ImmutableRouter<T extends Message> extends MessageRouter {
        Map<Class<T>, List<Consumer<T>>> routingTable();

        @Override
        @SuppressWarnings("unchecked")
        default <R extends Message> void route(R message) {
            routingTable()
                        .get(message.getClass())
                        .forEach(fn -> fn.accept((T) message));
        }
    }

    /// Entry in a route configuration.
    /// Used for building immutable routers with compile-time validation.
    ///
    /// @param <T> the message type
    interface Entry<T extends Message> {
        Stream<Tuple2<Class< ? extends T>, Consumer< ? extends T>>> entries();

        Class<T> type();

        Set<Class< ?>> validate();

        @SuppressWarnings("unchecked")
        default Result<MessageRouter> asRouter() {
            var validated = validate();
            if (!validated.isEmpty()) {
                var missing = validated.stream()
                                       .map(Class::getSimpleName)
                                       .collect(Collectors.joining(", "));
                return new InvalidMessageRouterConfiguration("Missing message types: " + missing).result();
            }
            record router<T extends Message>(Map<Class<T>, List<Consumer<T>>> routingTable) implements ImmutableRouter<T> {}
            var routingTable = new HashMap<Class<T>, List<Consumer<T>>>();
            entries()
                   .forEach(tuple -> routingTable.compute((Class<T>) tuple.first(),
                                                          (_, oldValue) -> merge(tuple, oldValue)));
            return Result.success(new router<>(routingTable));
        }

        @SuppressWarnings("unchecked")
        private static <T extends Message> List<Consumer<T>> merge(Tuple2<Class< ? extends T>, Consumer< ? extends T>> tuple,
                                                                   List<Consumer<T>> oldValue) {
            var list = oldValue == null
                       ? new ArrayList<Consumer<T>>()
                       : oldValue;
            list.add((Consumer<T>) tuple.last());
            return list;
        }

        /// Builder for sealed interface hierarchies.
        /// Validates that all permitted subclasses have routes.
        interface SealedBuilder<T extends Message> extends Entry<T> {
            static <T extends Message> SealedBuilder<T> from(Class<T> clazz) {
                record sealedBuilder<T extends Message>(Class<T> type,
                                                        List<Entry< ? extends T>> routes) implements SealedBuilder<T> {
                    @Override
                    @SuppressWarnings("unchecked")
                    public Stream<Tuple2<Class< ? extends T>, Consumer< ? extends T>>> entries() {
                        return routes.stream()
                                     .map(entry -> (Entry<T>) entry)
                                     .flatMap(Entry::entries);
                    }

                    @SafeVarargs
                    @Override
                    public final Entry<T> route(Entry<? extends T>... routes) {
                        routes()
                              .addAll(List.of(routes));
                        return this;
                    }

                    @Override
                    public Set<Class< ?>> validate() {
                        if (!type()
                                 .isSealed()) {
                            return mergeSubroutes(new HashSet<>());
                        }
                        var declared = routes()
                                             .stream()
                                             .map(Entry::type)
                                             .collect(Collectors.toSet());
                        var permitted = new HashSet<>(Set.of(type()
                                                                 .getPermittedSubclasses()));
                        permitted.removeAll(declared);
                        return mergeSubroutes(permitted);
                    }

                    private Set<Class< ?>> mergeSubroutes(Set<Class< ?>> local) {
                        routes()
                              .forEach(route -> local.addAll(route.validate()));
                        return local;
                    }
                }
                return new sealedBuilder<>(clazz, new ArrayList<>());
            }

            @SuppressWarnings("unchecked")
            Entry<T> route(Entry<? extends T>... routes);
        }

        /// Create a simple route entry for a specific message type.
        static <T extends Message> Entry<T> route(Class<T> type, Consumer<T> receiver) {
            record entry<T extends Message>(Class<T> type, Consumer<T> receiver) implements Entry<T> {
                @Override
                public Stream<Tuple2<Class< ? extends T>, Consumer< ? extends T>>> entries() {
                    return Stream.of(tuple(type(), receiver()));
                }

                @Override
                public Set<Class< ?>> validate() {
                    return Set.of();
                }
            }
            return new entry<>(type, receiver);
        }
    }

    /// Error for invalid router configuration.
    record InvalidMessageRouterConfiguration(String message) implements Cause {}
}
