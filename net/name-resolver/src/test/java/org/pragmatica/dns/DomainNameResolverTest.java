package org.pragmatica.dns;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.dns.ResolverErrors.UnknownDomain;

import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.Promise.all;
import static org.pragmatica.lang.io.Timeout.timeout;

class DomainNameResolverTest {
    private final DomainNameResolver resolver = DomainNameResolver.defaultResolver();

    @Test
    void resolveFewKnownDomains() {
        all(resolver.resolve("www.ibm.com"),
            resolver.resolve("www.google.com"),
            resolver.resolve("www.github.com"),
            resolver.resolve("www.twitter.com")
        ).map(Stream::of)
         .await(timeout(15).seconds())
         .onSuccess(list -> list.forEach(System.out::println))
         .onFailureDo(Assertions::fail);
    }

    //TODO: tag as SLOW
    @SuppressWarnings("deprecation")
    @Test
    void resultIsCachedAndTtlIsObserver() throws InterruptedException {
        //WARNING: test relies on the known TTL of the "www.ibm.com" domain (4 seconds), but this may change without notice
        resolver.resolve("www.ibm.com")
                .await(timeout(15).seconds())
                .onFailureDo(Assertions::fail);

        // Immediate request should return resolved value immediately
        var ttl = resolver.resolveCached("www.ibm.com")
                          .await(timeout(1).millis())
                          .onFailureDo(Assertions::fail)
                          .map(DomainAddress::ttl)
                          .unwrap();

        Thread.sleep(ttl.toMillis() + 200);

        // After TTL is expired, request for cached value should fail with UnknownDomain error
        resolver.resolveCached("www.ibm.com")
                .await(timeout(1).millis())
                .onSuccessDo(Assertions::fail)
                .onFailure(err -> assertEquals(UnknownDomain.class, err.getClass()));
    }
}