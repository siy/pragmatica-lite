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

    protected final ConnectibleProperties properties = new ConnectibleProperties();

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
    public static class ConnectibleProperties {

        String hostname = "localhost";
        int port = 5432;
        String username;
        String password;
        String database;
        int maxConnections = 20;
        int maxStatements = 20;
        DataConverter dataConverter;
        List<Converter<?>> converters = new ArrayList<>();
        boolean useSsl;
        String encoding = System.getProperty("pg.async.encoding", "utf-8");
        String validationQuery;

        public String getHostname() {
            return hostname;
        }

        public int getPort() {
            return port;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getDatabase() {
            return database;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public int getMaxStatements() {
            return maxStatements;
        }

        public boolean getUseSsl() {
            return useSsl;
        }

        public String getEncoding() {
            return encoding;
        }

        public DataConverter getDataConverter() {
            return dataConverter != null ? dataConverter : new DataConverter(converters, Charset.forName(encoding));
        }

        public String getValidationQuery() {
            return validationQuery;
        }
    }
}