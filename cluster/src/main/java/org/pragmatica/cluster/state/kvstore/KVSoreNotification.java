package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.cluster.state.StateMachineNotification;
import org.pragmatica.lang.Option;

public sealed interface KVSoreNotification extends StateMachineNotification {
    record ValuePut<K, V>(KVCommand.Put<K, V> cause, Option<V> oldValue) implements KVSoreNotification {}

    record ValueGet<K, V>(KVCommand.Get<K> cause, Option<V> value) implements KVSoreNotification {}

    record ValueRemove<K, V>(KVCommand.Remove<K> cause, Option<V> value) implements KVSoreNotification {}
}
