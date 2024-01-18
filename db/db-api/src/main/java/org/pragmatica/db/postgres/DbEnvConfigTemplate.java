package org.pragmatica.db.postgres;

import com.google.auto.service.AutoService;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple3;
import org.pragmatica.lang.type.FieldNames;
import org.pragmatica.lang.type.KeyToValue;
import org.pragmatica.lang.type.RecordTemplate;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.uri.IRI;

import java.util.List;

import static org.pragmatica.lang.Tuple.tuple;

//TODO: eventually it should be generated by annotation processor
public interface DbEnvConfigTemplate extends RecordTemplate<DbEnvConfig> {
    DbEnvConfigTemplate INSTANCE = new DbEnvConfigTemplate() {};
    static DbEnvConfigBuilder builder() {
        return url ->
            username ->
                password ->
                    maxConnections ->
                        maxStatements ->
                            useSsl ->
                                validationQuery ->
                                    encoding -> new DbEnvConfig(url,
                                                                username,
                                                                password,
                                                                maxConnections,
                                                                maxStatements,
                                                                useSsl,
                                                                validationQuery,
                                                                encoding);
    }

    interface DbEnvConfigBuilder {
        Username url(IRI url);

        default Username url(String url) {
            return url(IRI.fromString(url));
        }

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
            UseSsl maxStatements(int maxStatements);
        }

        interface UseSsl {
            ValidationQuery useSsl(boolean useSsl);
        }

        interface ValidationQuery {
            Encoding validationQuery(Option<String> validationQuery);
        }

        interface Encoding {
            DbEnvConfig encoding(Option<String> encoding);
        }
    }

    @Override
    default Result<DbEnvConfig> load(String prefix, KeyToValue mapping) {
        return Result.all(mapping.get(prefix, "url", new TypeToken<IRI>() {}),
                          mapping.get(prefix, "username", new TypeToken<String>() {}),
                          mapping.get(prefix, "password", new TypeToken<String>() {}),
                          mapping.get(prefix, "maxConnections", new TypeToken<Integer>() {}),
                          mapping.get(prefix, "maxStatements", new TypeToken<Integer>() {}),
                          mapping.get(prefix, "useSsl", new TypeToken<Boolean>() {}),
                          mapping.get(prefix, "validationQuery", new TypeToken<Option<String>>() {}),
                          mapping.get(prefix, "encoding", new TypeToken<Option<String>>() {}))
                     .map(DbEnvConfig::new);
    }

    @Override
    default FieldNames fieldNames() {
        return () -> FORMATTED_NAMES;
    }

    @Override
    default List<Tuple3<String, TypeToken<?>, Fn1<?, DbEnvConfig>>> extractors() {
        return VALUE_EXTRACTORS;
    }

    List<Tuple3<String, TypeToken<?>, Fn1<?, DbEnvConfig>>> VALUE_EXTRACTORS = List.of(
        tuple("url", new TypeToken<IRI>() {}, DbEnvConfig::url),
        tuple("username", new TypeToken<String>() {}, DbEnvConfig::username),
        tuple("password", new TypeToken<String>() {}, DbEnvConfig::password),
        tuple("maxConnections", new TypeToken<Integer>() {}, DbEnvConfig::maxConnections),
        tuple("maxStatements", new TypeToken<Integer>() {}, DbEnvConfig::maxStatements),
        tuple("useSsl", new TypeToken<Boolean>() {}, DbEnvConfig::useSsl),
        tuple("validationQuery", new TypeToken<Option<String>>() {}, DbEnvConfig::validationQuery),
        tuple("encoding", new TypeToken<Option<String>>() {}, DbEnvConfig::encoding)
    );

    String FORMATTED_NAMES = RecordTemplate.buildFormattedNames(VALUE_EXTRACTORS);
}
