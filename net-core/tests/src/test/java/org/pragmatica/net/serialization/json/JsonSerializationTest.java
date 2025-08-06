package org.pragmatica.net.serialization.json;

import org.junit.jupiter.api.Test;
import org.pragmatica.lang.type.TypeToken;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test actual JSON serialization functionality with Jackson provider
 */
class JsonSerializationTest {
    
    record TestRecord(String name, int value) {}
    
    @Test
    void shouldSerializeAndDeserializeSimpleObject() {
        var testObject = new TestRecord("test", 42);
        var codec = JsonCodec.of(TestRecord.class);
        
        var json = codec.toJson(testObject);
        assertThat(json).contains("test").contains("42");
        
        var deserialized = codec.fromJson(json, TestRecord.class);
        assertThat(deserialized).isEqualTo(testObject);
    }
    
    @Test
    void shouldWorkWithTypeToken() {
        var listData = List.of("hello", "world");
        var codec = JsonCodec.of(new TypeToken<List<String>>(){});
        
        var json = codec.toJson(listData);
        assertThat(json).contains("hello").contains("world");
        
        var deserialized = codec.fromJson(json, new TypeToken<List<String>>(){});
        assertThat(deserialized).containsExactly("hello", "world");
    }
    
    @Test
    void shouldWorkWithConvenienceMethods() {
        var listData = List.of("a", "b", "c");
        var codec = JsonCodec.listOf(String.class);
        
        var json = codec.toJson(listData);
        var deserialized = codec.fromJson(json, new TypeToken<List<String>>(){});
        assertThat(deserialized).containsExactly("a", "b", "c");
    }
    
    @Test
    void shouldWorkWithMaps() {
        var mapData = Map.of("key1", "value1", "key2", "value2");
        var codec = JsonCodec.mapOf(String.class, String.class);
        
        var json = codec.toJson(mapData);
        var deserialized = codec.fromJson(json, new TypeToken<Map<String, String>>(){});
        assertThat(deserialized).containsEntry("key1", "value1").containsEntry("key2", "value2");
    }
    
    @Test
    void shouldWorkWithResultHandling() {
        var testObject = new TestRecord("test", 42);
        var codec = JsonCodec.of(TestRecord.class);
        
        var jsonResult = codec.tryFromJson("{\"name\":\"test\",\"value\":42}", TestRecord.class);
        assertThat(jsonResult.isSuccess()).isTrue();
        assertThat(jsonResult.unwrap()).isEqualTo(testObject);
        
        var invalidJsonResult = codec.tryFromJson("{invalid json}", TestRecord.class);
        assertThat(invalidJsonResult.isFailure()).isTrue();
    }
}