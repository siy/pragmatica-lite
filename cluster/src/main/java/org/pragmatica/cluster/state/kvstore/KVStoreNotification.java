package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.cluster.state.StateMachineNotification;
import org.pragmatica.lang.Option;

public sealed interface KVStoreNotification extends StateMachineNotification {
    record ValuePut<K, V>(KVCommand.Put<K, V> cause, Option<V> oldValue) implements KVStoreNotification {}

    record ValueGet<K, V>(KVCommand.Get<K> cause, Option<V> value) implements KVStoreNotification {}

    record ValueRemove<K, V>(KVCommand.Remove<K> cause, Option<V> value) implements KVStoreNotification {}
}
