package org.pragmatica.net.http;

import java.util.*;

public final class HttpHeaders {
    private final Map<String, List<String>> headers;
    
    public HttpHeaders() {
        this.headers = new LinkedHashMap<>();
    }
    
    public HttpHeaders(Map<String, List<String>> headers) {
        this.headers = new LinkedHashMap<>();
        headers.forEach(this::addAll);
    }
    
    public HttpHeaders add(String name, String value) {
        
        headers.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>()).add(value);
        return this;
    }
    
    public HttpHeaders set(String name, String value) {
        
        headers.put(name.toLowerCase(), new ArrayList<>(List.of(value)));
        return this;
    }
    
    public HttpHeaders addAll(String name, List<String> values) {
        
        headers.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>()).addAll(values);
        return this;
    }
    
    public Optional<String> first(String name) {
        
        var values = headers.get(name.toLowerCase());
        return values != null && !values.isEmpty() ? Optional.of(values.get(0)) : Optional.empty();
    }
    
    public List<String> all(String name) {
        
        return headers.getOrDefault(name.toLowerCase(), Collections.emptyList());
    }
    
    public boolean contains(String name) {
        
        return headers.containsKey(name.toLowerCase());
    }
    
    public Set<String> names() {
        return Collections.unmodifiableSet(headers.keySet());
    }
    
    public Map<String, List<String>> asMap() {
        return Collections.unmodifiableMap(headers);
    }
    
    public boolean isEmpty() {
        return headers.isEmpty();
    }
    
    public int size() {
        return headers.size();
    }
    
    // Common header constants
    public static final String ACCEPT = "accept";
    public static final String ACCEPT_ENCODING = "accept-encoding";
    public static final String AUTHORIZATION = "authorization";
    public static final String CONTENT_TYPE = "content-type";
    public static final String CONTENT_LENGTH = "content-length";
    public static final String USER_AGENT = "user-agent";
    public static final String HOST = "host";
    public static final String CONNECTION = "connection";
    
    // Common content types
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_XML = "application/xml";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_HTML = "text/html";
    public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HttpHeaders that = (HttpHeaders) obj;
        return Objects.equals(headers, that.headers);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(headers);
    }
    
    @Override
    public String toString() {
        return headers.toString();
    }
}