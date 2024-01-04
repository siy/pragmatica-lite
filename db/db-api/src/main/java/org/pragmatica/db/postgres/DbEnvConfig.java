package org.pragmatica.db.postgres;

import org.pragmatica.lang.Option;
import org.pragmatica.uri.IRI;

//TODO: add SSL configuration
public record DbEnvConfig(IRI url, String username, String password, int maxConnections, int maxStatements,
                          Option<String> validationQuery, Option<String> encoding) {

}
