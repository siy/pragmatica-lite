package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.cluster.state.StateMachineNotification;
import org.pragmatica.lang.Option;

public sealed interface KVStateMachineNotification extends StateMachineNotification {
    record ValuePut<K, V>(KVCommand.Put<K, V> cause, Option<V> oldValue) implements KVStateMachineNotification {}

    record ValueGet<K, V>(KVCommand.Get<K> cause, Option<V> value) implements KVStateMachineNotification {}

    record ValueRemove<K, V>(KVCommand.Remove<K> cause, Option<V> value) implements KVStateMachineNotification {}
}
