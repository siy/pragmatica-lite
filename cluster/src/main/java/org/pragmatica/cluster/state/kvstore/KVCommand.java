package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.cluster.state.Command;

public sealed interface KVCommand extends Command {
    record Put<K, V>(K key, V value) implements KVCommand {}

    record Get<K>(K key) implements KVCommand {}

    record Remove<K>(K key) implements KVCommand {}
}
