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
import java.util.ArrayList;
import java.util.List;

import static io.netty.buffer.Unpooled.EMPTY_BUFFER;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.pragmatica.lang.Tuple.tuple;

public sealed interface DataContainer<T extends DataContainer<T>> {
    HttpStatus status();
    ByteBuf responseBody();
    List<Tuple2<CommonHeaders, String>> responseHeaders();

    T withHeaders(List<Tuple2<CommonHeaders, String>> headers);

    default T withHeader(CommonHeaders header, String value) {
        return withHeaders(List.of(tuple(header, value)));
    }

    record StringData(HttpStatus status, String value, List<Tuple2<CommonHeaders, String>> responseHeaders) implements DataContainer<StringData> {
        @Override
        public ByteBuf responseBody() {
            return wrappedBuffer(value.getBytes(StandardCharsets.UTF_8));
        }

        public static StringData from(String value) {
            return new StringData(HttpStatus.OK, value, List.of());
        }

        public static StringData from(HttpError value) {
            return new StringData(value.status(), value.message(), List.of());
        }

        public static StringData from(HttpStatus status, Cause cause) {
            return new StringData(status, cause.message(), List.of());
        }

        @Override
        public StringData withHeaders(List<Tuple2<CommonHeaders, String>> headers) {
            return new StringData(status, value, merge(responseHeaders, headers));
        }
    }

    record BinaryData(HttpStatus status, byte[] value, List<Tuple2<CommonHeaders, String>> responseHeaders) implements DataContainer<BinaryData> {
        @Override
        public ByteBuf responseBody() {
            return wrappedBuffer(value);
        }

        public static BinaryData from(byte[] value) {
            return new BinaryData(HttpStatus.OK, value, List.of());
        }

        @Override
        public BinaryData withHeaders(List<Tuple2<CommonHeaders, String>> headers) {
            return new BinaryData(status, value, merge(responseHeaders, headers));
        }
    }

    record ByteBufData(HttpStatus status, ByteBuf responseBody, List<Tuple2<CommonHeaders, String>> responseHeaders) implements DataContainer<ByteBufData> {
        public static ByteBufData from(ByteBuf value) {
            return new ByteBufData(HttpStatus.OK, value, List.of());
        }

        @Override
        public ByteBufData withHeaders(List<Tuple2<CommonHeaders, String>> headers) {
            return new ByteBufData(status, responseBody, merge(responseHeaders, headers));
        }
    }

    record RedirectData(HttpStatus status, String redirectUrl, List<Tuple2<CommonHeaders, String>> responseHeaders) implements DataContainer<RedirectData> {
        @Override
        public ByteBuf responseBody() {
            return EMPTY_BUFFER;
        }

        public static RedirectData from(Redirect redirect) {
            return new RedirectData(redirect.status(), URLEncoder.encode(redirect.url(), StandardCharsets.ISO_8859_1),
                                    List.of(tuple(CommonHeaders.LOCATION, redirect.url())));
        }

        @Override
        public RedirectData withHeaders(List<Tuple2<CommonHeaders, String>> headers) {
            return new RedirectData(status, redirectUrl, merge(responseHeaders, headers));
        }
    }

    private static List<Tuple2<CommonHeaders, String>> merge(List<Tuple2<CommonHeaders, String>> first, List<Tuple2<CommonHeaders, String>> second) {
        var list = new ArrayList<>(first);
        list.addAll(second);
        return list;
    }
}
