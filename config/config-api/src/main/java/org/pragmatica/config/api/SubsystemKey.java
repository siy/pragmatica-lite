package org.pragmatica.config.api;

public sealed interface SubsystemKey {
    String configPrefix();

    record HttpServerKey(String configPrefix) implements SubsystemKey {}
    record DnsResolverKey(String configPrefix) implements SubsystemKey {}
    record DatabaseKey(String configPrefix) implements SubsystemKey {}

    SubsystemKey HTTP_SERVER_KEY = new HttpServerKey("http-server");
    SubsystemKey DNS_RESOLVER_KEY = new HttpServerKey("name-resolver");
    SubsystemKey DATABASE_KEY = new HttpServerKey("database");

    non-sealed interface CustomSubsystemKey extends SubsystemKey {}
}
