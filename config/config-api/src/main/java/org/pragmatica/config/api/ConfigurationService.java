package org.pragmatica.config.api;

import org.pragmatica.lang.Result;

import java.util.List;
import java.util.function.Consumer;

/**
 * Generalized API to access configuration data.
 * <p>
 * The API assumes that configuration information is stored as a key-value pairs. The key is structured as a sequence of named segments separated by
 * dots. Each segment is a string consisting of alphanumeric characters, digits and underscores. Empty segments are not allowed, so the key cannot
 * start or end with a dot, and cannot contain more than one consecutive dots.
 * <p>
 * The API supports the following types of configuration parameters:
 * <ul>
 *     <li>String - strings of various size, potentially spanning across multiple lines</li>
 *     <li>Long - integer values which fit into Java {@link Long} type</li>
 *     <li>BigDecimal - numbers with fractional part</li>
 *     <li>Boolean - true/false</li>
 *     <li>OffsetDateTime - Date and time with offset from UTC</li>
 *     <li>LocalDateTime - Date and time in local time zone</li>
 *     <li>LocalDate - Only date (current time zone is assumed)</li>
 *     <li>LocalTime - Only time (current time zone is assumed)</li>
 *     <li>Duration</li>
 *     <li>Array of values of the same type</li>
 * </ul>
 */
public interface ConfigurationService {
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
    void listen(ParameterKey prefix, Consumer<ParameterKey> listener);

    List<String> supportedExtensions();
}
