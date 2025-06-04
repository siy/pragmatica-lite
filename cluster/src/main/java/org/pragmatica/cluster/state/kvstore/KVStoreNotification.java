package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.cluster.state.StateMachineNotification;
import org.pragmatica.lang.Option;

import java.util.regex.Pattern;

public sealed interface KVStoreNotification<K extends CharSequence> extends StateMachineNotification<KVCommand<K>> {
    default boolean matches(Pattern pattern) {
        return pattern.matcher(cause().key()).matches();
    }

    record ValuePut<K extends CharSequence, V>(KVCommand.Put<K, V> cause, Option<V> oldValue) implements KVStoreNotification<K> {}

    record ValueGet<K extends CharSequence, V>(KVCommand.Get<K> cause, Option<V> value) implements KVStoreNotification<K> {}

    record ValueRemove<K extends CharSequence, V>(KVCommand.Remove<K> cause, Option<V> value) implements KVStoreNotification<K> {}
}
