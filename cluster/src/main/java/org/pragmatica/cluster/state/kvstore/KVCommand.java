package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.cluster.state.Command;

public sealed interface KVCommand<K extends StructuredKey> extends Command {
    K key();

    record Put<K extends StructuredKey, V>(K key, V value) implements KVCommand<K> {}

    record Get<K extends StructuredKey>(K key) implements KVCommand<K> {}

    record Remove<K extends StructuredKey>(K key) implements KVCommand<K> {}
}
