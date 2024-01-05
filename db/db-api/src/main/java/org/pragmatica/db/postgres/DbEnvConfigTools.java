package org.pragmatica.db.postgres;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;
import org.pragmatica.lang.Tuple.Tuple3;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.uri.IRI;

import java.util.List;

import static org.pragmatica.lang.Tuple.tuple;

public interface DbEnvConfigTools {
    static DbEnvConfigBuilder builder() {
        return url -> username -> password -> maxConnections -> maxStatements -> validationQuery -> encoding -> () -> new DbEnvConfig(url,
                                                                                                                                      username,
                                                                                                                                      password,
                                                                                                                                      maxConnections,
                                                                                                                                      maxStatements,
                                                                                                                                      validationQuery,
                                                                                                                                      encoding);
    }

    interface DbEnvConfigBuilder {
        Username url(IRI url);

        interface Username {
            Password username(String username);
        }

        interface Password {
            MaxConnections password(String password);
        }

        interface MaxConnections {
            MaxStatements maxConnections(int maxConnections);
        }

        interface MaxStatements {
            ValidationQuery maxStatements(int maxStatements);
        }

        interface ValidationQuery {
            Encoding validationQuery(Option<String> validationQuery);

        }

        interface Encoding {
            Build encoding(Option<String> encoding);

        }

        interface Build {
            DbEnvConfig build();
        }
    }


    static List<Tuple2<String, TypeToken<?>>> fields() {
        return List.of(
            tuple("url", new TypeToken<IRI>() {}),
            tuple("username", new TypeToken<String>() {}),
            tuple("password", new TypeToken<String>() {}),
            tuple("maxConnections", new TypeToken<Integer>() {}),
            tuple("maxStatements", new TypeToken<Integer>() {}),
            tuple("validationQuery", new TypeToken<Option<String>>() {}),
            tuple("encoding", new TypeToken<Option<String>>() {})
        );
    }

    static List<Tuple3<String, TypeToken<?>, ?>> values(DbEnvConfig config) {
        return List.of(
            tuple("url", new TypeToken<IRI>() {}, config.url()),
            tuple("username", new TypeToken<String>() {}, config.username()),
            tuple("password", new TypeToken<String>() {}, config.password()),
            tuple("maxConnections", new TypeToken<Integer>() {}, config.maxConnections()),
            tuple("maxStatements", new TypeToken<Integer>() {}, config.maxStatements()),
            tuple("validationQuery", new TypeToken<Option<String>>() {}, config.validationQuery()),
            tuple("encoding", new TypeToken<Option<String>>() {}, config.encoding()));
    }

    static Result<DbEnvConfig> load(KeyToValue mapping) {
        return Result.all(mapping.get("url", new TypeToken<IRI>() {}),
                          mapping.get("username", new TypeToken<String>() {}),
                          mapping.get("password", new TypeToken<String>() {}),
                          mapping.get("maxConnections", new TypeToken<Integer>() {}),
                          mapping.get("maxStatements", new TypeToken<Integer>() {}),
                          mapping.get("validationQuery", new TypeToken<Option<String>>() {}),
                          mapping.get("encoding", new TypeToken<Option<String>>() {}))
                     .map(DbEnvConfig::new);
    }
}
