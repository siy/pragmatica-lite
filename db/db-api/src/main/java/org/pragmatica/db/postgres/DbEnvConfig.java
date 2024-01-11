package org.pragmatica.db.postgres;

import org.pragmatica.lang.Option;
import org.pragmatica.uri.IRI;

//TODO: add SSL configuration
public record DbEnvConfig(IRI url, String username, String password, int maxConnections, int maxStatements,
                          boolean useSsl,
                          Option<String> validationQuery, Option<String> encoding) {

    public DbEnvConfig withMaxConnections(int maxConnections) {
        return new DbEnvConfig(url(), username(), password(), maxConnections, maxStatements(), useSsl(), validationQuery(), encoding());
    }
}