package org.pragmatica.http.client;

import org.junit.jupiter.api.Test;
import org.pragmatica.http.CommonContentTypes;
import org.pragmatica.http.ContentType;
import org.pragmatica.http.protocol.HttpHeaders;
import org.pragmatica.http.protocol.HttpMethod;
import org.pragmatica.lang.Option;
import org.pragmatica.uri.IRI;

import static org.junit.jupiter.api.Assertions.*;

class HttpClientTest {
    @Test
    void clientSmokeTest() {
        var client = HttpClient.create(HttpClientConfiguration.allDefaults());
        var request = HttpClientRequest.httpClientRequest(HttpMethod.GET,
                                                          CommonContentTypes.TEXT_PLAIN,
                                                          IRI.fromString("https://www.google.com/"),
                                                          HttpHeaders.httpHeaders(), Option.none(), Option.none());
        var response = client.call(request)
                             .await()
                             .onSuccess(System.out::println);
    }
}