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
    
    public HttpHeaders add(CommonHttpHeaders header, String value) {
        return add(header.headerName(), value);
    }
    
    public HttpHeaders set(String name, String value) {
        headers.put(name.toLowerCase(), new ArrayList<>(List.of(value)));
        return this;
    }
    
    public HttpHeaders set(CommonHttpHeaders header, String value) {
        return set(header.headerName(), value);
    }
    
    public HttpHeaders addAll(String name, List<String> values) {
        headers.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>()).addAll(values);
        return this;
    }
    
    public HttpHeaders addAll(CommonHttpHeaders header, List<String> values) {
        return addAll(header.headerName(), values);
    }
    
    public Optional<String> first(String name) {
        var values = headers.get(name.toLowerCase());
        return values != null && !values.isEmpty() ? Optional.of(values.get(0)) : Optional.empty();
    }
    
    public Optional<String> first(CommonHttpHeaders header) {
        return first(header.headerName());
    }
    
    public List<String> all(String name) {
        return headers.getOrDefault(name.toLowerCase(), Collections.emptyList());
    }
    
    public List<String> all(CommonHttpHeaders header) {
        return all(header.headerName());
    }
    
    public boolean contains(String name) {
        return headers.containsKey(name.toLowerCase());
    }
    
    public boolean contains(CommonHttpHeaders header) {
        return contains(header.headerName());
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