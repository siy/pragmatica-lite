package org.pragmatica.config.api;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for configuration format readers (pluggable component).
 */
public interface ConfigFormatReader {
    /**
     * Retrieve a configuration parameter.
     *
     * @param key  The key of the parameter to retrieve.
     * @param type The type of the parameter to retrieve.
     *
     * @return The parameter value or error, if parameter is not defined or its type does not match requested.
     * @see ParameterType
     */
    <T> Result<T> get(ParameterKey key, ParameterType<T> type);

    /**
     * Listen for changes of a configuration parameters. Notifications are sent for changes in all parameters with the given prefix. The
     * {@link ParameterKey} passed to the listener is the full key of the parameter that has changed.
     *
     * @param prefix   The prefix of the parameters to listen for.
     * @param listener The listener to be called when a parameter with the given prefix changes.
     */
    default void listen(ParameterKey prefix, Consumer<ParameterKey> listener) {
        //TODO: Implement
        //No-op as for now
    }

    List<String> supportedExtensions();

    Result<Unit> read(String content);
}
