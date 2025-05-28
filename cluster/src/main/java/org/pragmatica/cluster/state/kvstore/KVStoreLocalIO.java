package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.message.Message;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public sealed interface KVStoreLocalIO extends Message.Local {
    sealed interface Request extends KVStoreLocalIO {
        record Find(Pattern pattern) implements Request {
            public <K extends CharSequence, V> boolean matches(Map.Entry<K, V> entry) {
                return pattern().matcher(entry.getKey()).matches();
            }
        }
    }

    sealed interface Response extends KVStoreLocalIO {
        record FoundEntries<K, V>(List<Map.Entry<K, V>> entries) implements Response {}
    }
}
