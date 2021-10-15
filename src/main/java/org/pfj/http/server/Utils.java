package org.pfj.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class Utils {
    private Utils() {}

    private static final Pattern MULTISLASH = Pattern.compile("/+");

    public static ByteBuf asByteBuf(String content) {
        return Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8));
    }

    public static String normalize(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "/";
        }

        var stringPath = MULTISLASH.matcher("/" + path.strip()).replaceAll("/");
        var index = stringPath.lastIndexOf('/');

        if (index < (stringPath.length() - 1)) {
            return stringPath + "/";
        } else {
            return stringPath;
        }
    }
}
