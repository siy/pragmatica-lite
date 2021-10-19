package org.pfj.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class Utils {
    private Utils() {
    }

    private static final Pattern MULTISLASH = Pattern.compile("/+");

    public static ByteBuf asByteBuf(String content) {
        return Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8));
    }

    public static String normalize(String fullPath) {
        if (fullPath == null) {
            return "/";
        }

        var query = fullPath.indexOf('?');
        var path = query >= 0 ? fullPath.substring(0, query).strip() : fullPath.strip();

        if (path.isBlank() || "/".equals(path)) {
            return "/";
        }

        var stringPath = MULTISLASH.matcher("/" + path).replaceAll("/");
        var index = stringPath.lastIndexOf('/');

        if (index < (stringPath.length() - 1)) {
            return stringPath + "/";
        } else {
            return stringPath;
        }
    }

    /**
     * Solution below is taken from https://stackoverflow.com/a/29141814/5349078
     * <br/>
     * <br/>
     * <b>WARNING:</b> Suitable only for single thread access!
     * <br/>
     * <br/>
     * Usage:
     * <pre>
     * Supplier<Baz> fieldBaz = lazy(() -> fieldBaz=value(expensiveInitBaz()));
     * Supplier<Goo> fieldGoo = lazy(() -> fieldGoo=value(expensiveInitGoo()));
     * Supplier<Eep> fieldEep = lazy(() -> fieldEep=value(expensiveInitEep()));
     * </pre>
     */
    public interface Lazy<T> extends Supplier<T> {
        Supplier<T> init();

        default T get() {
            return init().get();
        }
    }

    public static <U> Supplier<U> lazy(Lazy<U> lazy) {
        return lazy;
    }

    public static <T> Supplier<T> value(T value) {
        return () -> value;
    }
}
