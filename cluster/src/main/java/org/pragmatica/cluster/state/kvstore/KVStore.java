package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.cluster.state.StateMachine;
import org.pragmatica.cluster.state.kvstore.KVCommand.Get;
import org.pragmatica.cluster.state.kvstore.KVCommand.Put;
import org.pragmatica.cluster.state.kvstore.KVCommand.Remove;
import org.pragmatica.cluster.state.kvstore.KVStoreNotification.ValueGet;
import org.pragmatica.cluster.state.kvstore.KVStoreNotification.ValuePut;
import org.pragmatica.cluster.state.kvstore.KVStoreNotification.ValueRemove;
import org.pragmatica.cluster.state.kvstore.KVStoreLocalIO.Request.Find;
import org.pragmatica.cluster.state.kvstore.KVStoreLocalIO.Response.FoundEntries;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.message.MessageRouter;
import org.pragmatica.net.serialization.Deserializer;
import org.pragmatica.net.serialization.Serializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KVStore<K extends StructuredKey, V> implements StateMachine<KVCommand<K>> {
    private final Map<K, V> storage = new ConcurrentHashMap<>();
    private final Serializer serializer;
    private final Deserializer deserializer;
    private final MessageRouter router;

    public KVStore(MessageRouter router, Serializer serializer, Deserializer deserializer) {
        this.router = router;
        this.serializer = serializer;
        this.deserializer = deserializer;

        this.router.addRoute(Find.class, this::find);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Option<V> process(KVCommand command) {
        return switch (command) {
            case Get<?> get -> handleGet((Get<K>) get);
            case Put<?, ?> put -> handlePut((Put<K, V>) put);
            case Remove<?> remove -> handleRemove((Remove<K>) remove);
        };
    }

    private Option<V> handleGet(Get<K> get) {
        var value = Option.option(storage.get(get.key()));

        router.route(new ValueGet<>(get, value));

        return value;
    }

    private Option<V> handlePut(Put<K, V> put) {
        var oldValue = Option.option(storage.put(put.key(), put.value()));

        router.route(new ValuePut<>(put, oldValue));

        return oldValue;
    }

    private Option<V> handleRemove(Remove<K> remove) {
        var oldValue = Option.option(storage.remove(remove.key()));

        router.route(new ValueRemove<>(remove, oldValue));

        return oldValue;
    }

    @Override
    public Result<byte[]> makeSnapshot() {
        return Result.lift(Causes::fromThrowable,
                           () -> serializer.encode(new HashMap<>(storage)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Result<Unit> restoreSnapshot(byte[] snapshot) {
        return Result.lift(Causes::fromThrowable, () -> deserializer.decode(snapshot))
                     .map(map -> (Map<K, V>) map)
                     .onSuccessRun(this::notifyRemoveAll)
                     .onSuccessRun(storage::clear)
                     .onSuccess(storage::putAll)
                     .onSuccessRun(this::notifyPutAll)
                     .mapToUnit();
    }

    private void notifyRemoveAll() {
        storage.forEach((key, value) -> router.route(new ValueRemove<>(new Remove<>(key), Option.some(value))));
    }

    private void notifyPutAll() {
        storage.forEach((key, value) -> router.route(new ValuePut<>(new Put<>(key, value), Option.none())));
    }

    @Override
    public void reset() {
        notifyRemoveAll();
        storage.clear();
    }

    public Map<K, V> snapshot() {
        return new HashMap<>(storage);
    }

    private void find(Find find) {
        router.routeAsync(() -> new FoundEntries<>(storage.entrySet()
                                                          .stream()
                                                          .filter(find::matches)
                                                          .toList()));
    }
}
