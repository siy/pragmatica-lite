package org.pfj.http.server;

import org.pfj.http.server.error.CauseMapper;
import org.pfj.http.server.serialization.DefaultSerializer;
import org.pfj.http.server.serialization.Serializer;

import static org.pfj.lang.Option.option;

public class Configuration {
    private static final int DEFAULT_PORT = 8000;
    private static final CauseMapper DEFAULT_CAUSE_MAPPER = CauseMapper::defaultConverter;
    private static final Serializer DEFAULT_SERIALIZER = DefaultSerializer.withDefault();

    private final int port;
    private final Serializer serializer;
    private final CauseMapper causeMapper;

    private Configuration(int port, Serializer serializer, CauseMapper causeMapper) {
        this.port = port;
        this.serializer = serializer;
        this.causeMapper = causeMapper;
    }

    public static Builder atPort(int port) {
        return new Builder(port);
    }

    public static Builder atDefaultPort() {
        return new Builder(DEFAULT_PORT);
    }

    public static final class Builder {
        private final int port;
        private Serializer serializer = null;
        private CauseMapper causeMapper = null;

        private Builder(int port) {
            this.port = port;
        }

        public Builder with(CauseMapper causeMapper) {
            this.causeMapper = causeMapper;
            return this;
        }

        public Builder with(Serializer serializer) {
            this.serializer = serializer;
            return this;
        }

        public Configuration build() {
            return new Configuration(
                port,
                option(serializer).or(DEFAULT_SERIALIZER),
                option(causeMapper).or(DEFAULT_CAUSE_MAPPER)
            );
        }
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
}
