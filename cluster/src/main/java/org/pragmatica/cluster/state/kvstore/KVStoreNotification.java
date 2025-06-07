package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.cluster.state.StateMachineNotification;
import org.pragmatica.lang.Option;

public sealed interface KVStoreNotification<K extends StructuredKey> extends StateMachineNotification<KVCommand<K>> {
    default boolean matches(StructuredPattern pattern) {
        return cause().key().matches(pattern);
    }

    record ValuePut<K extends StructuredKey, V>(KVCommand.Put<K, V> cause, Option<V> oldValue) implements KVStoreNotification<K> {}

    record ValueGet<K extends StructuredKey, V>(KVCommand.Get<K> cause, Option<V> value) implements KVStoreNotification<K> {}

    record ValueRemove<K extends StructuredKey, V>(KVCommand.Remove<K> cause, Option<V> value) implements KVStoreNotification<K> {}
}
