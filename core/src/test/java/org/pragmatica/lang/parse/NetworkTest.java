/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
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

package org.pragmatica.lang.parse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NetworkTest {

    @Test
    void testParseURLSuccess() {
        Network.parseURL("https://example.com/path?query=value")
                .onFailureRun(Assertions::fail)
                .onSuccess(url -> {
                    assertEquals("https", url.getProtocol());
                    assertEquals("example.com", url.getHost());
                    assertEquals("/path", url.getPath());
                });
    }

    @Test
    void testParseURLFailure() {
        Network.parseURL("not a valid url")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseURLNull() {
        Network.parseURL(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseURISuccess() {
        Network.parseURI("https://example.com:8080/path?query=value#fragment")
                .onFailureRun(Assertions::fail)
                .onSuccess(uri -> {
                    assertEquals("https", uri.getScheme());
                    assertEquals("example.com", uri.getHost());
                    assertEquals(8080, uri.getPort());
                    assertEquals("/path", uri.getPath());
                    assertEquals("query=value", uri.getQuery());
                    assertEquals("fragment", uri.getFragment());
                });
    }

    @Test
    void testParseURIFailure() {
        Network.parseURI("http://[invalid")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseURINull() {
        Network.parseURI(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseURIEmpty() {
        // Empty string is a valid URI
        Network.parseURI("")
                .onFailureRun(Assertions::fail)
                .onSuccess(uri -> assertEquals("", uri.toString()));
    }

    @Test
    void testParseUUIDSuccess() {
        var expected = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Network.parseUUID("550e8400-e29b-41d4-a716-446655440000")
                .onFailureRun(Assertions::fail)
                .onSuccess(uuid -> assertEquals(expected, uuid));
    }

    @Test
    void testParseUUIDFailure() {
        Network.parseUUID("not-a-uuid")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseUUIDNull() {
        Network.parseUUID(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseUUIDEmpty() {
        Network.parseUUID("")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseInetAddressSuccess() {
        Network.parseInetAddress("localhost")
                .onFailureRun(Assertions::fail)
                .onSuccess(addr -> assertTrue(addr.isLoopbackAddress() || "localhost".equals(addr.getHostName())));
    }

    @Test
    void testParseInetAddressIPv4() {
        Network.parseInetAddress("127.0.0.1")
                .onFailureRun(Assertions::fail)
                .onSuccess(addr -> assertEquals("127.0.0.1", addr.getHostAddress()));
    }

    @Test
    void testParseInetAddressIPv6() {
        Network.parseInetAddress("::1")
                .onFailureRun(Assertions::fail)
                .onSuccess(addr -> {
                    assertTrue(addr.getHostAddress().contains(":"));
                    assertTrue(addr.isLoopbackAddress());
                });
    }

    @Test
    void testParseInetAddressFailure() {
        // Use malformed IPv6 which is guaranteed to fail (not DNS-dependent)
        Network.parseInetAddress("[::invalid")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseInetAddressNull() {
        // null is treated as localhost by InetAddress
        Network.parseInetAddress(null)
                .onFailureRun(Assertions::fail)
                .onSuccess(addr -> assertTrue(addr.isLoopbackAddress()));
    }

    // Edge cases

    @Test
    void testParseUUIDUppercase() {
        var expected = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Network.parseUUID("550E8400-E29B-41D4-A716-446655440000")
                .onFailureRun(Assertions::fail)
                .onSuccess(uuid -> assertEquals(expected, uuid));
    }

    @Test
    void testParseURLWithCredentials() {
        Network.parseURL("https://user:pass@example.com/path")
                .onFailureRun(Assertions::fail)
                .onSuccess(url -> {
                    assertEquals("https", url.getProtocol());
                    assertEquals("user:pass", url.getUserInfo());
                    assertEquals("example.com", url.getHost());
                });
    }

    @Test
    void testParseURIRelative() {
        Network.parseURI("../path/to/resource")
                .onFailureRun(Assertions::fail)
                .onSuccess(uri -> {
                    assertNull(uri.getScheme());
                    assertEquals("../path/to/resource", uri.getPath());
                });
    }

    @Test
    void testParseURIMailto() {
        Network.parseURI("mailto:test@example.com")
                .onFailureRun(Assertions::fail)
                .onSuccess(uri -> assertEquals("mailto", uri.getScheme()));
    }
}
