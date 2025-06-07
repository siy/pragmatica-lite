package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.message.Message;

import java.util.List;
import java.util.Map;

public sealed interface KVStoreLocalIO extends Message.Local {
    sealed interface Request extends KVStoreLocalIO {
        record Find(StructuredPattern pattern) implements Request {
            public <K extends StructuredKey, V> boolean matches(Map.Entry<K, V> entry) {
                return entry.getKey().matches(pattern);
            }
        }
    }

    sealed interface Response extends KVStoreLocalIO {
        record FoundEntries<K extends StructuredKey, V>(List<Map.Entry<K, V>> entries) implements Response {}
    }
}
