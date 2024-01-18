package org.pragmatica.http.server;

import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.ssl.SslContext;
import org.pragmatica.http.codec.CustomCodec;
import org.pragmatica.http.codec.JsonCodec;
import org.pragmatica.lang.Functions;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple;
import org.pragmatica.lang.type.FieldNames;
import org.pragmatica.lang.type.KeyToValue;
import org.pragmatica.lang.type.RecordTemplate;
import org.pragmatica.lang.type.TypeToken;

import java.net.InetAddress;
import java.util.List;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.some;
import static org.pragmatica.lang.Tuple.tuple;

public interface HttpServerConfigTemplate extends RecordTemplate<HttpServerConfig> {
    HttpServerConfigTemplate INSTANCE = new HttpServerConfigTemplate() {};

    static HttpServerConfigurationBuilder builder() {
        return port -> bindAddress -> sendBufferSize -> receiveBufferSize -> maxContentLen -> nativeTransport -> customCodec -> jsonCodec -> sslContext -> corsConfig ->
            new HttpServerConfig(
                port,
                bindAddress,
                sendBufferSize,
                receiveBufferSize,
                maxContentLen,
                nativeTransport,
                customCodec,
                jsonCodec,
                sslContext,
                corsConfig);
    }

    interface HttpServerConfigurationBuilder {
        BindAddress port(int port);

        interface BindAddress {
            SendBufferSize bindAddress(Option<InetAddress> bindAddress);
        }

        interface SendBufferSize {
            ReceiveBufferSize sendBufferSize(int sendBufferSize);
        }

        interface ReceiveBufferSize {
            MaxContentLen receiveBufferSize(int receiveBufferSize);
        }

        interface MaxContentLen {
            NativeTransport maxContentLen(int maxContentLen);
        }

        interface NativeTransport {
            CustomCodecStage nativeTransport(boolean nativeTransport);
        }

        interface CustomCodecStage {
            JsonCodecStage customCodec(Option<CustomCodec> customCodec);
        }

        interface JsonCodecStage {
            SslContextStage jsonCodec(Option<JsonCodec> jsonCodec);
        }

        interface SslContextStage {
            CorsConfigStage sslContext(Option<SslContext> sslContext);
        }

        interface CorsConfigStage {
            HttpServerConfig corsConfig(Option<CorsConfig> corsConfig);
        }
    }

    interface With {
        int port();

        Option<InetAddress> bindAddress();

        int sendBufferSize();

        int receiveBufferSize();

        int maxContentLen();

        boolean nativeTransport();

        Option<CustomCodec> customCodec();

        Option<JsonCodec> jsonCodec();

        Option<SslContext> sslContext();

        Option<CorsConfig> corsConfig();

        default HttpServerConfig withPort(int port) {
            return new HttpServerConfig(port, bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport(),
                                        customCodec(), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfig withBindAddress(InetAddress host) {
            return new HttpServerConfig(port(), some(host), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport(),
                                        customCodec(), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfig withSendBufferSize(int sendBufferSize) {
            return new HttpServerConfig(port(), bindAddress(), sendBufferSize, receiveBufferSize(), maxContentLen(), nativeTransport(),
                                        customCodec(), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfig withReceiveBufferSize(int receiveBufferSize) {
            return new HttpServerConfig(port(), bindAddress(), sendBufferSize(), receiveBufferSize, maxContentLen(), nativeTransport(),
                                        customCodec(), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfig withMaxContentLen(int maxContentLen) {
            return new HttpServerConfig(port(), bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen, nativeTransport(),
                                        customCodec(), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfig withNativeTransport(boolean nativeTransport) {
            return new HttpServerConfig(port(), bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport,
                                        customCodec(), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfig withJsonCodec(JsonCodec jsonCodec) {
            return new HttpServerConfig(port(), bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport(),
                                        customCodec(), some(jsonCodec), sslContext(), corsConfig());
        }

        default HttpServerConfig withCustomCodec(CustomCodec customCodec) {
            return new HttpServerConfig(port(), bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport(),
                                        some(customCodec), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfig withSslContext(SslContext sslContext) {
            return new HttpServerConfig(port(), bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport(),
                                        customCodec(), jsonCodec(), some(sslContext), corsConfig());
        }

        default HttpServerConfig withCorsConfig(CorsConfig corsConfig) {
            return new HttpServerConfig(port(), bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport(),
                                        customCodec(), jsonCodec(), sslContext(), some(corsConfig));
        }
    }

    @Override
    default Result<HttpServerConfig> load(String prefix, KeyToValue mapping) {
        return Result.all(mapping.get(prefix, "port", new TypeToken<Integer>() {}),
                          mapping.get(prefix, "bindAddress", new TypeToken<Option<InetAddress>>() {}),
                          mapping.get(prefix, "sendBufferSize", new TypeToken<Integer>() {}),
                          mapping.get(prefix, "receiveBufferSize", new TypeToken<Integer>() {}),
                          mapping.get(prefix, "maxContentLen", new TypeToken<Integer>() {}),
                          mapping.get(prefix, "nativeTransport", new TypeToken<Boolean>() {}))
                     //TODO: load remaining fields via dedicated records.
//                          mapping.get(prefix, "customCodec", new TypeToken<Option<CustomCodec>>() {}),
//                          mapping.get(prefix, "jsonCodec", new TypeToken<Option<JsonCodec>>() {}),
//                          mapping.get(prefix, "sslContext", new TypeToken<Option<SslContext>>() {}),
//                          mapping.get(prefix, "corsConfig", new TypeToken<Option<CorsConfig>>() {}))
                     .map((port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport) ->
                              new HttpServerConfig(port,
                                                   bindAddress,
                                                   sendBufferSize,
                                                   receiveBufferSize,
                                                   maxContentLen,
                                                   nativeTransport,
                                                   none(),
                                                   none(),
                                                   none(),
                                                   none()));
    }

    @Override
    default FieldNames fieldNames() {
        return () -> FORMATTED_NAMES;
    }

    @Override
    default List<Tuple.Tuple3<String, TypeToken<?>, Functions.Fn1<?, HttpServerConfig>>> extractors() {
        return VALUE_EXTRACTORS;
    }

    List<Tuple.Tuple3<String, TypeToken<?>, Functions.Fn1<?, HttpServerConfig>>> VALUE_EXTRACTORS = List.of(
        tuple("port", new TypeToken<Integer>() {}, HttpServerConfig::port),
        tuple("bindAddress", new TypeToken<Option<InetAddress>>() {}, HttpServerConfig::bindAddress),
        tuple("sendBufferSize", new TypeToken<Integer>() {}, HttpServerConfig::sendBufferSize),
        tuple("receiveBufferSize", new TypeToken<Integer>() {}, HttpServerConfig::receiveBufferSize),
        tuple("maxContentLen", new TypeToken<Integer>() {}, HttpServerConfig::maxContentLen),
        tuple("nativeTransport", new TypeToken<Boolean>() {}, HttpServerConfig::nativeTransport),
        tuple("customCodec", new TypeToken<Option<CustomCodec>>() {}, HttpServerConfig::customCodec),
        tuple("jsonCodec", new TypeToken<Option<JsonCodec>>() {}, HttpServerConfig::jsonCodec),
        tuple("sslContext", new TypeToken<Option<SslContext>>() {}, HttpServerConfig::sslContext),
        tuple("corsConfig", new TypeToken<Option<CorsConfig>>() {}, HttpServerConfig::corsConfig));

    String FORMATTED_NAMES = RecordTemplate.buildFormattedNames(VALUE_EXTRACTORS);
}

