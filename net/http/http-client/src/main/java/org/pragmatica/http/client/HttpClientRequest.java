package org.pragmatica.http.client;

import org.pragmatica.http.ContentType;
import org.pragmatica.http.protocol.HttpHeaders;
import org.pragmatica.http.protocol.HttpMethod;
import org.pragmatica.lang.Option;
import org.pragmatica.http.protocol.QueryParameters;
import org.pragmatica.uri.IRI;

public record HttpClientRequest<T>(HttpMethod method,
                                   IRI iri,
                                   Option<QueryParameters>parameters,
                                   Option<HttpHeaders> headers,
                                   Option<T> body,
                                   ContentType contentType) {

}
