package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.net.serialization.Deserializer;
import org.pragmatica.net.serialization.Serializer;
import org.pragmatica.cluster.state.StateMachine;
import org.pragmatica.cluster.state.StateMachineNotification;
import org.pragmatica.cluster.state.kvstore.KVCommand.Get;
import org.pragmatica.cluster.state.kvstore.KVCommand.Put;
import org.pragmatica.cluster.state.kvstore.KVCommand.Remove;
import org.pragmatica.cluster.state.kvstore.KVStateMachineNotification.ValueGet;
import org.pragmatica.cluster.state.kvstore.KVStateMachineNotification.ValuePut;
import org.pragmatica.cluster.state.kvstore.KVStateMachineNotification.ValueRemove;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class KVStore<K, V> implements StateMachine<KVCommand> {
    private final Map<K, V> storage = new ConcurrentHashMap<>();
    private final Serializer serializer;
    private final Deserializer deserializer;
    private Consumer<? super StateMachineNotification> observer = _ -> {};

    public KVStore(Serializer serializer, Deserializer deserializer) {
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @SuppressWarnings("unchecked")
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

        observer.accept(new ValueGet<>(get, value));

        return value;
    }

    private Option<V> handlePut(Put<K, V> put) {
        var oldValue = Option.option(storage.put(put.key(), put.value()));

        observer.accept(new ValuePut<>(put, oldValue));

        return oldValue;
    }

    private Option<V> handleRemove(Remove<K> remove) {
        var oldValue = Option.option(storage.remove(remove.key()));

        observer.accept(new ValueRemove<>(remove, oldValue));

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
                     .onSuccessRun(storage::clear)
                     .onSuccess(storage::putAll)
                     .map(_ -> Unit.unit());
    }

    @Override
    public void observeStateChanges(Consumer<? super StateMachineNotification> observer) {
        this.observer = observer;
    }

    @Override
    public void reset() {
        storage.clear();
    }

    public Map<K, V> snapshot() {
        return new HashMap<>(storage);
    }
}
