package org.pragmatica.net.http;

import java.time.Duration;

public record HttpClientConfig(
    Duration connectTimeout,
    Duration requestTimeout,
    Duration readTimeout,
    int maxConnections,
    int maxConnectionsPerHost,
    boolean followRedirects,
    String userAgent,
    HttpHeaders defaultHeaders
) {
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static HttpClientConfig defaults() {
        return builder().build();
    }
    
    public static class Builder {
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofSeconds(30);
        private Duration readTimeout = Duration.ofSeconds(30);
        private int maxConnections = 100;
        private int maxConnectionsPerHost = 10;
        private boolean followRedirects = true;
        private String userAgent = "pragmatica-http-client/1.0";
        private HttpHeaders defaultHeaders = new HttpHeaders();
        
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }
        
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }
        
        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }
        
        public Builder maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }
        
        public Builder maxConnectionsPerHost(int maxConnectionsPerHost) {
            this.maxConnectionsPerHost = maxConnectionsPerHost;
            return this;
        }
        
        public Builder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public Builder defaultHeaders(HttpHeaders defaultHeaders) {
            this.defaultHeaders = defaultHeaders;
            return this;
        }
        
        public Builder defaultHeader(String name, String value) {
            this.defaultHeaders.add(name, value);
            return this;
        }
        
        public HttpClientConfig build() {
            return new HttpClientConfig(
                connectTimeout,
                requestTimeout,
                readTimeout,
                maxConnections,
                maxConnectionsPerHost,
                followRedirects,
                userAgent,
                defaultHeaders
            );
        }
    }
}