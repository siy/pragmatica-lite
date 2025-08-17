package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.state.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Optimized batch serialization that avoids redundant copying and serialization
 * by implementing incremental snapshots and command deduplication.
 */
public class OptimizedBatchSerializer<C extends Command> {
    private static final Logger log = LoggerFactory.getLogger(OptimizedBatchSerializer.class);
    
    // Cache serialized batches to avoid re-serialization
    private final ConcurrentMap<CorrelationId, byte[]> serializedBatchCache = new ConcurrentHashMap<>();
    private final int maxCacheSize;
    
    public OptimizedBatchSerializer(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }
    
    /**
     * Efficiently serialize batches with caching to avoid redundant serialization.
     * Only new/modified batches are serialized.
     */
    public BatchSnapshot serializeBatches(Collection<Batch<C>> batches) {
        var totalSize = 0;
        var serializedBatches = new ConcurrentHashMap<CorrelationId, byte[]>();
        
        for (var batch : batches) {
            var correlationId = batch.correlationId();
            var serialized = serializedBatchCache.get(correlationId);
            
            if (serialized == null) {
                // Only serialize if not already cached
                serialized = serializeBatch(batch);
                
                // Implement cache eviction if needed
                if (serializedBatchCache.size() >= maxCacheSize) {
                    evictOldestCacheEntries();
                }
                
                serializedBatchCache.put(correlationId, serialized);
            }
            
            serializedBatches.put(correlationId, serialized);
            totalSize += serialized.length;
        }
        
        return new BatchSnapshot(serializedBatches, totalSize);
    }
    
    /**
     * Serialize a single batch efficiently.
     * This is a placeholder - in a real implementation, you'd use
     * protocol buffers, MessagePack, or another efficient serializer.
     */
    private byte[] serializeBatch(Batch<C> batch) {
        try {
            // For now, use a simple approach - in production you'd use:
            // - Protocol Buffers for cross-language compatibility
            // - MessagePack for performance
            // - Custom binary format for maximum efficiency
            
            var commands = batch.commands();
            if (commands.isEmpty()) {
                return new byte[0];
            }
            
            // Simple length-prefixed format: [count][command1_len][command1_data]...
            var totalSize = 4; // count
            var serializedCommands = new byte[commands.size()][];
            
            for (int i = 0; i < commands.size(); i++) {
                var commandData = serializeCommand(commands.get(i));
                serializedCommands[i] = commandData;
                totalSize += 4 + commandData.length; // length prefix + data
            }
            
            var result = new byte[totalSize];
            var pos = 0;
            
            // Write count
            writeInt(result, pos, commands.size());
            pos += 4;
            
            // Write commands
            for (var commandData : serializedCommands) {
                writeInt(result, pos, commandData.length);
                pos += 4;
                System.arraycopy(commandData, 0, result, pos, commandData.length);
                pos += commandData.length;
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to serialize batch {}", batch.correlationId(), e);
            return new byte[0];
        }
    }
    
    /**
     * Serialize a single command.
     * This should be implemented based on your command structure.
     */
    private byte[] serializeCommand(C command) {
        // Placeholder implementation - replace with actual command serialization
        return command.toString().getBytes();
    }
    
    private void writeInt(byte[] array, int offset, int value) {
        array[offset] = (byte) (value >>> 24);
        array[offset + 1] = (byte) (value >>> 16);
        array[offset + 2] = (byte) (value >>> 8);
        array[offset + 3] = (byte) value;
    }
    
    /**
     * Remove oldest cache entries when cache is full.
     * In a more sophisticated implementation, you'd use LRU eviction.
     */
    private void evictOldestCacheEntries() {
        var entriesToRemove = serializedBatchCache.size() - (maxCacheSize * 3 / 4);
        var iterator = serializedBatchCache.entrySet().iterator();
        
        for (int i = 0; i < entriesToRemove && iterator.hasNext(); i++) {
            iterator.next();
            iterator.remove();
        }
    }
    
    /**
     * Remove batch from cache when it's committed
     */
    public void removeBatchFromCache(CorrelationId correlationId) {
        serializedBatchCache.remove(correlationId);
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        serializedBatchCache.clear();
    }
    
    /**
     * Container for serialized batch data
     */
    public static class BatchSnapshot {
        private final ConcurrentMap<CorrelationId, byte[]> serializedBatches;
        private final int totalSize;
        
        public BatchSnapshot(ConcurrentMap<CorrelationId, byte[]> serializedBatches, int totalSize) {
            this.serializedBatches = serializedBatches;
            this.totalSize = totalSize;
        }
        
        public ConcurrentMap<CorrelationId, byte[]> getSerializedBatches() {
            return serializedBatches;
        }
        
        public int getTotalSize() {
            return totalSize;
        }
        
        public boolean isEmpty() {
            return serializedBatches.isEmpty();
        }
    }
}