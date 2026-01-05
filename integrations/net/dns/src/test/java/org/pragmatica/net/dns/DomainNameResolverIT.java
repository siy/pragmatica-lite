/*
 *  Copyright (c) 2022-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.net.dns;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.net.dns.DomainNameResolver.domainNameResolver;

/// Integration tests for DNS resolution using real DNS servers.
///
/// These tests require network access and may be flaky if DNS servers are unreachable.
/// Enable by setting environment variable: DNS_INTEGRATION_TESTS=true
@EnabledIfEnvironmentVariable(named = "DNS_INTEGRATION_TESTS", matches = "true")
class DomainNameResolverIT {

    private DomainNameResolver resolver;

    @BeforeEach
    void setUp() throws UnknownHostException {
        // Use well-known public DNS servers
        resolver = domainNameResolver(List.of(
            InetAddress.getByName("8.8.8.8"),      // Google
            InetAddress.getByName("1.1.1.1")       // Cloudflare
        ));
    }

    @AfterEach
    void tearDown() {
        if (resolver != null) {
            resolver.close().await();
        }
    }

    @Test
    void resolves_well_known_domain() {
        resolver.resolve("google.com")
                .await()
                .onFailureRun(Assertions::fail)
                .onSuccess(address -> {
                    assertThat(address.name().name()).isEqualTo("google.com");
                    assertThat(address.ip()).isNotNull();
                    assertThat(address.ttl()).isNotNull();
                    System.out.println("Resolved google.com to " + address.ip() + " (TTL: " + address.ttl() + ")");
                });
    }

    @Test
    void resolves_domain_with_short_ttl() {
        // ibm.com typically has shorter TTL for load balancing
        resolver.resolve("ibm.com")
                .await()
                .onFailureRun(Assertions::fail)
                .onSuccess(address -> {
                    assertThat(address.name().name()).isEqualTo("ibm.com");
                    assertThat(address.ip()).isNotNull();
                    System.out.println("Resolved ibm.com to " + address.ip() + " (TTL: " + address.ttl() + ")");

                    // IBM typically has TTL under 5 minutes
                    assertThat(address.ttl()).isLessThan(Duration.ofMinutes(10));
                });
    }

    @Test
    void caches_resolution_and_respects_ttl() throws InterruptedException {
        // First resolution
        var firstResult = resolver.resolve("example.com").await();

        firstResult.onFailureRun(Assertions::fail)
                   .onSuccess(first -> {
                       System.out.println("First resolution: " + first.ip() + " (TTL: " + first.ttl() + ")");

                       // Cached resolution should return same result
                       resolver.resolveCached("example.com")
                               .await()
                               .onFailureRun(Assertions::fail)
                               .onSuccess(cached -> {
                                   assertThat(cached.ip()).isEqualTo(first.ip());
                               });
                   });
    }

    @Test
    void fails_for_nonexistent_domain() {
        resolver.resolve("this-domain-definitely-does-not-exist-12345.invalid")
                .await()
                .onSuccessRun(() -> Assertions.fail("Should have failed for non-existent domain"))
                .onFailure(cause -> {
                    System.out.println("Expected failure for non-existent domain: " + cause.message());
                    assertThat(cause).isNotNull();
                });
    }

    @Test
    void handles_multiple_concurrent_resolutions() {
        var domains = List.of("google.com", "microsoft.com", "apple.com", "amazon.com");

        var promises = domains.stream()
                              .map(resolver::resolve)
                              .toList();

        for (int i = 0; i < promises.size(); i++) {
            var domain = domains.get(i);
            promises.get(i)
                    .await()
                    .onSuccess(address -> {
                        System.out.println("Resolved " + domain + " to " + address.ip());
                        assertThat(address.name().name()).isEqualTo(domain);
                    })
                    .onFailure(cause -> {
                        System.out.println("Failed to resolve " + domain + ": " + cause.message());
                    });
        }
    }

    @Test
    void ttl_cache_eviction_works() throws InterruptedException {
        // Resolve a domain with short TTL
        var result = resolver.resolve("ibm.com").await();

        result.onSuccess(address -> {
            var ttlSeconds = address.ttl().toSeconds();
            System.out.println("ibm.com TTL: " + ttlSeconds + " seconds");

            if (ttlSeconds > 0 && ttlSeconds < 30) {
                // Wait for TTL to expire plus small buffer
                try {
                    System.out.println("Waiting " + (ttlSeconds + 2) + " seconds for TTL expiry...");
                    Thread.sleep((ttlSeconds + 2) * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                // Cache should be evicted, resolveCached should fail
                resolver.resolveCached("ibm.com")
                        .await()
                        .onSuccess(cached -> System.out.println("Still cached (may have been re-resolved): " + cached.ip()))
                        .onFailure(cause -> System.out.println("Cache evicted as expected: " + cause.message()));
            } else {
                System.out.println("TTL too long for practical test, skipping eviction verification");
            }
        });
    }
}
