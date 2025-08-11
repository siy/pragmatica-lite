package org.pragmatica.message;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.pragmatica.lang.Tuple.tuple;

/// **NOTE**: This implementation assumes that the instance is configured and then used without
/// changes.
public sealed interface MessageRouter {
    <T extends Message> void route(T message);

    default <T extends Message> void routeAsync(Supplier<T> messageSupplier) {
        Promise.async(() -> route(messageSupplier.get()));
    }

    static MutableRouter mutable() {
        return new MutableRouter.SimpleMutableRouter<>(new ConcurrentHashMap<>());
    }

    sealed interface MutableRouter extends MessageRouter {
        <T extends Message> MessageRouter addRoute(Class<? extends T> messageType, Consumer<? extends T> receiver);

        record SimpleMutableRouter<T extends Message>(
                ConcurrentMap<Class<T>, List<Consumer<T>>> routingTable) implements
                MutableRouter {

            private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);

            @Override
            public <R extends Message> void route(R message) {
                Option.option(routingTable.get(message.getClass()))
                      .onPresent(list -> list.forEach(fn -> fn.accept((T) message)))
                      .onEmpty(() -> log.warn("No route for message type: {}", message.getClass().getSimpleName()));
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R extends Message> MessageRouter addRoute(Class<? extends R> messageType,
                                                              Consumer<? extends R> receiver) {
                routingTable.computeIfAbsent((Class<T>) messageType, _ -> new ArrayList<>())
                            .add((Consumer<T>) receiver);

                return this;
            }
        }
    }

    non-sealed interface ImmutableRouter<T extends Message> extends MessageRouter {
        Map<Class<T>, List<Consumer<T>>> routingTable();

        @SuppressWarnings("unchecked")
        default <R extends Message> void route(R message) {
            routingTable().get(message.getClass()).forEach(fn -> fn.accept((T) message));
        }
    }

    interface Entry<T extends Message> {
        Stream<Tuple2<Class<? extends T>, Consumer<? extends T>>> entries();

        Class<T> type();

        Set<Class<?>> validate();

        @SuppressWarnings("unchecked")
        default Result<MessageRouter> asRouter() {
            var validated = validate();

            if (!validated.isEmpty()) {
                var missing = validated.stream()
                                       .map(Class::getSimpleName)
                                       .collect(Collectors.joining(", "));
                return new InvalidMessageRouterConfiguration("Missing message types: " + missing).result();
            }

            record router<T extends Message>(
                    Map<Class<T>, List<Consumer<T>>> routingTable) implements ImmutableRouter<T> {}

            var routingTable = new HashMap<Class<T>, List<Consumer<T>>>();

            entries().forEach(tuple -> routingTable.compute((Class<T>) tuple.first(),
                                                            (_, oldValue) -> merge(tuple, oldValue)));

            return Result.success(new router<>(routingTable));
        }

        @SuppressWarnings("unchecked")
        private static <T extends Message> List<Consumer<T>> merge(Tuple2<Class<? extends T>, Consumer<? extends T>> tuple,
                                                                   List<Consumer<T>> oldValue) {
            var list = oldValue == null ? new ArrayList<Consumer<T>>() : oldValue;
            list.add((Consumer<T>) tuple.last());
            return list;
        }

        interface SealedBuilder<T extends Message> extends Entry<T> {
            static <T extends Message> SealedBuilder<T> from(Class<T> clazz) {
                record sealedBuilder<T extends Message>(Class<T> type,
                                                        List<Entry<? extends T>> routes) implements SealedBuilder<T> {
                    @SuppressWarnings("unchecked")
                    @Override
                    public Stream<Tuple2<Class<? extends T>, Consumer<? extends T>>> entries() {
                        return routes.stream()
                                     .map(entry -> (Entry<T>) entry)
                                     .flatMap(Entry::entries);
                    }

                    @SafeVarargs
                    @Override
                    public final Entry<T> route(Entry<? extends T>... routes) {
                        routes().addAll(List.of(routes));

                        return this;
                    }

                    @Override
                    public Set<Class<?>> validate() {
                        if (!type().isSealed()) {
                            return mergeSubroutes(new HashSet<>());
                        }

                        var declared = routes().stream()
                                               .map(Entry::type)
                                               .collect(Collectors.toSet());
                        var permitted = new HashSet<>(Set.of(type().getPermittedSubclasses()));

                        permitted.removeAll(declared);

                        return mergeSubroutes(permitted);
                    }

                    private Set<Class<?>> mergeSubroutes(Set<Class<?>> local) {
                        routes().forEach(route -> local.addAll(route.validate()));
                        return local;
                    }
                }

                return new sealedBuilder<>(clazz, new ArrayList<>());
            }

            @SuppressWarnings("unchecked")
            Entry<T> route(Entry<? extends T>... routes);
        }

        static <T extends Message> Entry<T> route(Class<T> type, Consumer<T> receiver) {
            record entry<T extends Message>(Class<T> type, Consumer<T> receiver) implements Entry<T> {

                @Override
                public Stream<Tuple2<Class<? extends T>, Consumer<? extends T>>> entries() {
                    return Stream.of(tuple(type(), receiver()));
                }

                @Override
                public Set<Class<?>> validate() {
                    return Set.of(); // Route is always valid
                }
            }

            return new entry<>(type, receiver);
        }
    }

    record InvalidMessageRouterConfiguration(String message) implements Cause {}
}
