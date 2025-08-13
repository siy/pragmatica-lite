package org.pragmatica.net.serialization.json;

import org.junit.jupiter.api.Test;
import org.pragmatica.lang.type.TypeToken;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Clean test that verifies TypeToken support exists in JSON interfaces
 * without needing to implement all interface methods.
 */
class CleanTypeTokenTest {
    
    @Test 
    void typeTokenStaticMethodsShouldExistAndCompile() {
        // Test that static factory methods with TypeToken exist and compile
        var listToken = new TypeToken<List<String>>(){};
        var mapToken = new TypeToken<Map<String, Integer>>(){};
        
        // These should compile and throw exceptions due to no providers
        assertThatThrownBy(() -> JsonCodec.of(listToken))
            .hasMessageContaining("No JSON providers");
            
        assertThatThrownBy(() -> JsonCodec.of(mapToken))  
            .hasMessageContaining("No JSON providers");
            
        assertThatThrownBy(() -> JsonRegistry.codec(listToken))
            .hasMessageContaining("No JSON providers");
            
        assertThatThrownBy(() -> JsonRegistry.codec(mapToken))
            .hasMessageContaining("No JSON providers");
    }
    
    @Test
    void convenienceMethodsShouldExist() {
        // Test that convenience static methods exist and compile
        assertThatThrownBy(() -> JsonCodec.listOf(String.class))
            .hasMessageContaining("No JSON providers");
            
        assertThatThrownBy(() -> JsonCodec.mapOf(String.class, Integer.class))
            .hasMessageContaining("No JSON providers");
    }
}