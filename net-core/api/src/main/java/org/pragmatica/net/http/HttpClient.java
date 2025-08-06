package org.pragmatica.net.http;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

public interface HttpClient {
    
    <T> Promise<HttpResponse<T>> send(HttpRequest<T> request);
    
    default HttpRequestBuilder request() {
        return HttpRequestBuilder.create(this);
    }
    
    default HttpResource resource(String baseUrl) {
        return HttpResource.create(this, baseUrl);
    }
    
    Promise<Unit> start();
    
    Promise<Unit> stop();
    
    static HttpClient create() {
        return new org.pragmatica.net.http.netty.NettyHttpClient(HttpClientConfig.defaults());
    }
    
    static HttpClient create(HttpClientConfig config) {
        return new org.pragmatica.net.http.netty.NettyHttpClient(config);
    }
}