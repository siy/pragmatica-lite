package org.pragmatica.http.client;

import org.pragmatica.http.ContentType;
import org.pragmatica.http.protocol.HttpHeaders;
import org.pragmatica.http.protocol.HttpMethod;
import org.pragmatica.http.protocol.QueryParameters;
import org.pragmatica.lang.Option;
import org.pragmatica.uri.IRI;

import static org.pragmatica.http.client.HttpClientRequest.Builder1.builder1;

public interface HttpClientRequest<T> {
    HttpMethod method();
    ContentType contentType();
    IRI iri();
    HttpHeaders headers();
    Option<QueryParameters> parameters();
    Option<T> body();

    static <T> HttpClientRequest<T> httpClientRequest(HttpMethod method, ContentType contentType, IRI iri, HttpHeaders headers,
                                                      Option<QueryParameters> parameters, Option<T> body) {
        record httpClientRequest<T>(HttpMethod method, ContentType contentType, IRI iri, HttpHeaders headers,
                                 Option<QueryParameters> parameters, Option<T> body) implements HttpClientRequest<T> {}

        return new httpClientRequest<>(method, contentType, iri, headers, parameters, body);
    }

    static Builder1 post(IRI iri) {                 //C
        return builder1(HttpMethod.POST, iri);
    }

    static Builder1 get(IRI iri) {                  //R
        return builder1(HttpMethod.GET, iri);
    }

    static Builder1 put(IRI iri) {                  //U
        return builder1(HttpMethod.POST, iri);
    }

    static Builder1 delete(IRI iri) {               //D
        return builder1(HttpMethod.DELETE, iri);
    }

    interface Builder1 {
        static Builder1 builder1(HttpMethod method, IRI iri) {
            record builder1(HttpMethod method, IRI iri) implements Builder1 {}

            return new builder1(method, iri);
        }
    }
}
