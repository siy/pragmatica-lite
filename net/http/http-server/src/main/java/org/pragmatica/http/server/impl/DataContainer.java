package org.pragmatica.http.server.impl;

import io.netty.buffer.ByteBuf;
import org.pragmatica.http.HttpError;
import org.pragmatica.http.protocol.CommonHeaders;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.routing.Redirect;
import org.pragmatica.lang.Result.Cause;
import org.pragmatica.lang.Tuple.Tuple2;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.netty.buffer.Unpooled.EMPTY_BUFFER;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.pragmatica.lang.Tuple.tuple;

//TODO: make adding headers to response possible for all data types
public sealed interface DataContainer {
    HttpStatus status();
    ByteBuf responseBody();
    default List<Tuple2<CommonHeaders, String>> responseHeaders() {
        return List.of();
    }

    record StringData(HttpStatus status, String value) implements DataContainer {
        @Override
        public ByteBuf responseBody() {
            return wrappedBuffer(value.getBytes(StandardCharsets.UTF_8));
        }

        public static StringData from(String value) {
            return new StringData(HttpStatus.OK, value);
        }

        public static StringData from(HttpError value) {
            return new StringData(value.status(), value.message());
        }

        public static StringData from(HttpStatus status, Cause cause) {
            return new StringData(status, cause.message());
        }
    }

    record BinaryData(HttpStatus status, byte[] value) implements DataContainer {
        @Override
        public ByteBuf responseBody() {
            return wrappedBuffer(value);
        }

        public static BinaryData from(byte[] value) {
            return new BinaryData(HttpStatus.OK, value);
        }
    }

    record ByteBufData(HttpStatus status, ByteBuf responseBody) implements DataContainer {
        public static ByteBufData from(ByteBuf value) {
            return new ByteBufData(HttpStatus.OK, value);
        }
    }

    record RedirectData(HttpStatus status, String redirectUrl) implements DataContainer {
        @Override
        public ByteBuf responseBody() {
            return EMPTY_BUFFER;
        }

        @Override
        public List<Tuple2<CommonHeaders, String>> responseHeaders() {
            return List.of(tuple(CommonHeaders.LOCATION, redirectUrl));
        }

        public static RedirectData from(Redirect redirect) {
            return new RedirectData(redirect.status(), URLEncoder.encode(redirect.url(), StandardCharsets.ISO_8859_1));
        }
    }
}
