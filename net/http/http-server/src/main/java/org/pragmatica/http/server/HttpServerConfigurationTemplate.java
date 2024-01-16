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

public interface HttpServerConfigurationTemplate extends RecordTemplate<HttpServerConfiguration> {
    HttpServerConfigurationTemplate INSTANCE = new HttpServerConfigurationTemplate() {};

    static HttpServerConfigurationBuilder builder() {
        return port -> bindAddress -> sendBufferSize -> receiveBufferSize -> maxContentLen -> nativeTransport -> customCodec -> jsonCodec -> sslContext -> corsConfig ->
            new HttpServerConfiguration(
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
            HttpServerConfiguration corsConfig(Option<CorsConfig> corsConfig);
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

        default HttpServerConfiguration withPort(int port) {
            return new HttpServerConfiguration(port, bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport(),
                                               customCodec(), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfiguration withBindAddress(InetAddress host) {
            return new HttpServerConfiguration(port(), some(host), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport(),
                                               customCodec(), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfiguration withSendBufferSize(int sendBufferSize) {
            return new HttpServerConfiguration(port(), bindAddress(), sendBufferSize, receiveBufferSize(), maxContentLen(), nativeTransport(),
                                               customCodec(), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfiguration withReceiveBufferSize(int receiveBufferSize) {
            return new HttpServerConfiguration(port(), bindAddress(), sendBufferSize(), receiveBufferSize, maxContentLen(), nativeTransport(),
                                               customCodec(), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfiguration withMaxContentLen(int maxContentLen) {
            return new HttpServerConfiguration(port(), bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen, nativeTransport(),
                                               customCodec(), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfiguration withNativeTransport(boolean nativeTransport) {
            return new HttpServerConfiguration(port(), bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport,
                                               customCodec(), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfiguration withJsonCodec(JsonCodec jsonCodec) {
            return new HttpServerConfiguration(port(), bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport(),
                                               customCodec(), some(jsonCodec), sslContext(), corsConfig());
        }

        default HttpServerConfiguration withCustomCodec(CustomCodec customCodec) {
            return new HttpServerConfiguration(port(), bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport(),
                                               some(customCodec), jsonCodec(), sslContext(), corsConfig());
        }

        default HttpServerConfiguration withSslContext(SslContext sslContext) {
            return new HttpServerConfiguration(port(), bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport(),
                                               customCodec(), jsonCodec(), some(sslContext), corsConfig());
        }

        default HttpServerConfiguration withCorsConfig(CorsConfig corsConfig) {
            return new HttpServerConfiguration(port(), bindAddress(), sendBufferSize(), receiveBufferSize(), maxContentLen(), nativeTransport(),
                                               customCodec(), jsonCodec(), sslContext(), some(corsConfig));
        }
    }

    @Override
    default Result<HttpServerConfiguration> load(String prefix, KeyToValue mapping) {
        return Result.all(mapping.get(prefix, "port", new TypeToken<Integer>() {}),
                          mapping.get(prefix, "bindAddress", new TypeToken<Option<InetAddress>>() {}),
                          mapping.get(prefix, "sendBufferSize", new TypeToken<Integer>() {}),
                          mapping.get(prefix, "receiveBufferSize", new TypeToken<Integer>() {}),
                          mapping.get(prefix, "maxContentLen", new TypeToken<Integer>() {}),
                          mapping.get(prefix, "nativeTransport", new TypeToken<Boolean>() {}))
//                          mapping.get(prefix, "customCodec", new TypeToken<Option<CustomCodec>>() {}),
//                          mapping.get(prefix, "jsonCodec", new TypeToken<Option<JsonCodec>>() {}),
//                          mapping.get(prefix, "sslContext", new TypeToken<Option<SslContext>>() {}),
//                          mapping.get(prefix, "corsConfig", new TypeToken<Option<CorsConfig>>() {}))
                     .map((port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport) ->
                              new HttpServerConfiguration(port,
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
    default List<Tuple.Tuple3<String, TypeToken<?>, Functions.Fn1<?, HttpServerConfiguration>>> extractors() {
        return VALUE_EXTRACTORS;
    }

    List<Tuple.Tuple3<String, TypeToken<?>, Functions.Fn1<?, HttpServerConfiguration>>> VALUE_EXTRACTORS = List.of(
        tuple("port", new TypeToken<Integer>() {}, HttpServerConfiguration::port),
        tuple("bindAddress", new TypeToken<Option<InetAddress>>() {}, HttpServerConfiguration::bindAddress),
        tuple("sendBufferSize", new TypeToken<Integer>() {}, HttpServerConfiguration::sendBufferSize),
        tuple("receiveBufferSize", new TypeToken<Integer>() {}, HttpServerConfiguration::receiveBufferSize),
        tuple("maxContentLen", new TypeToken<Integer>() {}, HttpServerConfiguration::maxContentLen),
        tuple("nativeTransport", new TypeToken<Boolean>() {}, HttpServerConfiguration::nativeTransport),
        tuple("customCodec", new TypeToken<Option<CustomCodec>>() {}, HttpServerConfiguration::customCodec),
        tuple("jsonCodec", new TypeToken<Option<JsonCodec>>() {}, HttpServerConfiguration::jsonCodec),
        tuple("sslContext", new TypeToken<Option<SslContext>>() {}, HttpServerConfiguration::sslContext),
        tuple("corsConfig", new TypeToken<Option<CorsConfig>>() {}, HttpServerConfiguration::corsConfig));

    String FORMATTED_NAMES = RecordTemplate.buildFormattedNames(VALUE_EXTRACTORS);
}

