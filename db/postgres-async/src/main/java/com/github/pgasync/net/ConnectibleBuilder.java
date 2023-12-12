/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pgasync.net;

import com.github.pgasync.conversion.DataConverter;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builder for creating {@link Connectible} instances.
 *
 * @author Antti Laisi
 * @author Marat Gainullin
 */
public abstract class ConnectibleBuilder {

    protected final ConnectibleConfiguration properties = new ConnectibleConfiguration();

    /**
     * @return Pool ready for use
     */
    public abstract Connectible pool();

    /**
     * @return Pool ready for use
     */
    public abstract Connectible plain();

    public ConnectibleBuilder hostname(String hostname) {
        properties.hostname = hostname;
        return this;
    }

    public ConnectibleBuilder port(int port) {
        properties.port = port;
        return this;
    }

    public ConnectibleBuilder username(String username) {
        properties.username = username;
        return this;
    }

    public ConnectibleBuilder password(String password) {
        properties.password = password;
        return this;
    }

    public ConnectibleBuilder database(String database) {
        properties.database = database;
        return this;
    }

    public ConnectibleBuilder maxConnections(int maxConnections) {
        properties.maxConnections = maxConnections;
        return this;
    }

    public ConnectibleBuilder maxStatements(int maxStatements) {
        properties.maxStatements = maxStatements;
        return this;
    }

    public ConnectibleBuilder converters(Converter<?>... converters) {
        Collections.addAll(properties.converters, converters);
        return this;
    }

    public ConnectibleBuilder dataConverter(DataConverter dataConverter) {
        properties.dataConverter = dataConverter;
        return this;
    }

    public ConnectibleBuilder ssl(boolean ssl) {
        properties.useSsl = ssl;
        return this;
    }

    public ConnectibleBuilder validationQuery(String validationQuery) {
        properties.validationQuery = validationQuery;
        return this;
    }

    public ConnectibleBuilder encoding(String value) {
        properties.encoding = value;
        return this;
    }

    /**
     * Configuration for a connectible.
     */
    public static class ConnectibleConfiguration {
        private String hostname = "localhost";
        private int port = 5432;
        private String username;
        private String password;
        private String database;
        private int maxConnections = 20;
        private int maxStatements = 20;
        private DataConverter dataConverter;
        private final List<Converter<?>> converters = new ArrayList<>();
        private boolean useSsl;
        private String encoding = System.getProperty("pg.async.encoding", "utf-8");
        private String validationQuery;

        public String hostname() {
            return hostname;
        }

        public int port() {
            return port;
        }

        public String username() {
            return username;
        }

        public String password() {
            return password;
        }

        public String database() {
            return database;
        }

        public int maxConnections() {
            return maxConnections;
        }

        public int maxStatements() {
            return maxStatements;
        }

        public boolean useSsl() {
            return useSsl;
        }

        public String encoding() {
            return encoding;
        }

        public DataConverter dataConverter() {
            return dataConverter != null ? dataConverter : new DataConverter(converters, Charset.forName(encoding));
        }

        public String validationQuery() {
            return validationQuery;
        }
    }
}