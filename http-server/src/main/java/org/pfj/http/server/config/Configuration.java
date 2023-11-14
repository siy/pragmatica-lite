package org.pfj.http.server.config;

import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import org.pfj.http.server.config.serialization.DefaultSerializer;
import org.pfj.http.server.config.serialization.Serializer;
import org.pfj.http.server.error.CauseMapper;
import org.pragmatica.lang.Option;

import static org.pragmatica.lang.Option.option;

public class Configuration {
    private final int port;
    private final Serializer serializer;
    private final CauseMapper causeMapper;
    private final int sendBufferSize;
    private final int receiveBufferSize;
    private final int maxContentLen;
    private final LogLevel logLevel;
    private final boolean enableNative;
    private final Option<SslContext> sslContext;
    private final Option<CorsConfig> corsConfig;

    private Configuration(Builder builder) {
        this.port = builder.port;
        this.serializer = builder.serializer;
        this.causeMapper = builder.causeMapper;
        this.sendBufferSize = builder.sendBufferSize;
        this.receiveBufferSize = builder.receiveBufferSize;
        this.maxContentLen = builder.maxContentLen;
        this.logLevel = builder.logLevel;
        this.enableNative = builder.enableNative;
        this.sslContext = option(builder.sslContext);
        this.corsConfig = option(builder.corsConfig);
    }

    public static Configuration allDefaults() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Serializer serializer() {
        return serializer;
    }

    public int port() {
        return port;
    }

    public CauseMapper causeMapper() {
        return causeMapper;
    }

    public int sendBufferSize() {
        return sendBufferSize;
    }

    public int receiveBufferSize() {
        return receiveBufferSize;
    }

    public int maxContentLen() {
        return maxContentLen;
    }

    public LogLevel logLevel() {
        return logLevel;
    }

    public boolean enableNative() {
        return enableNative;
    }

    public Option<SslContext> sslContext() {
        return sslContext;
    }

    public Option<CorsConfig> corsConfig() {
        return corsConfig;
    }

    public static final class Builder {
        private static final int KB = 1024;
        private static final int MB = KB * KB;

        private int port = 8000;
        private boolean enableNative = true;
        private int sendBufferSize = MB;
        private int receiveBufferSize = 32 * KB;
        private int maxContentLen = 10 * MB;
        private Serializer serializer = DefaultSerializer.withDefault();
        private CauseMapper causeMapper = CauseMapper::defaultConverter;
        private LogLevel logLevel = LogLevel.DEBUG;
        private SslContext sslContext = null;
        private CorsConfig corsConfig = null;

        private Builder() {
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder with(CauseMapper causeMapper) {
            this.causeMapper = causeMapper;
            return this;
        }

        public Builder with(Serializer serializer) {
            this.serializer = serializer;
            return this;
        }

        public Builder withSendBufferSize(int size) {
            this.sendBufferSize = size;
            return this;
        }

        public Builder withReceiveBufferSize(int size) {
            this.receiveBufferSize = size;
            return this;
        }

        public Builder withLogLevel(LogLevel level) {
            this.logLevel = level;
            return this;
        }

        public Builder withNativeTransport(boolean enable) {
            this.enableNative = enable;
            return this;
        }

        public Builder withSsl(SslContext context) {
            this.sslContext = context;
            return this;
        }

        public Builder withCors(CorsConfig corsConfig) {
            this.corsConfig = corsConfig;
            return this;
        }

        public Configuration build() {
            return new Configuration(this);
        }
    }
}
