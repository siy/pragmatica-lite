package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.cluster.state.Command;

public sealed interface KVCommand<K extends CharSequence> extends Command {
    K key();

    record Put<K extends CharSequence, V>(K key, V value) implements KVCommand<K> {}

    record Get<K extends CharSequence>(K key) implements KVCommand<K> {}

    record Remove<K extends CharSequence>(K key) implements KVCommand<K> {}
}
