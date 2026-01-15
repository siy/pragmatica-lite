package org.pragmatica.http.routing;

import java.util.function.Supplier;

/**
 * Utility methods for HTTP routing.
 */
public sealed interface Utils {
    /**
     * Lazy initialization pattern for fields.
     * <p>
     * <b>WARNING:</b> Suitable only for single thread access and only for field initialization!
     * <p>
     * Usage:
     * <pre>
     * Supplier&lt;Baz&gt; fieldBaz = lazy(() -&gt; fieldBaz=value(expensiveInitBaz()));
     * </pre>
     */
    interface Lazy<T> extends Supplier<T> {
        Supplier<T> init();

        default T get() {
            return init()
                       .get();
        }
    }

    static <U> Supplier<U> lazy(Lazy<U> lazy) {
        return lazy;
    }

    static <T> Supplier<T> value(T value) {
        return () -> value;
    }

    record unused() implements Utils {}
}
