package org.pragmatica.http.client;

public interface WebClient {
    static WebClient create(WebClientConfiguration configuration) {
        return new WebClientImpl(configuration);
    }

//    <T> WebClientRequest<T> request(String method, String uri, Class<T> responseType);
}

record WebClientImpl(WebClientConfiguration configuration) implements WebClient {

}