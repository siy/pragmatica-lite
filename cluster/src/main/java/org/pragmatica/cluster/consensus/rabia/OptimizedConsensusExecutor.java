package org.pragmatica.cluster.consensus.rabia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optimized executor for consensus operations that reduces single-threaded bottlenecks
 * while maintaining the required ordering guarantees for consensus protocol.
 */
public class OptimizedConsensusExecutor {
    private static final Logger log = LoggerFactory.getLogger(OptimizedConsensusExecutor.class);
    
    private final ExecutorService consensusExecutor;
    private final ExecutorService fastPathExecutor;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    
    // Separate queues for different operation types
    private final BlockingQueue<ConsensusTask> criticalTasks = new LinkedBlockingQueue<>();
    private final BlockingQueue<ConsensusTask> fastPathTasks = new LinkedBlockingQueue<>();
    
    public OptimizedConsensusExecutor() {
        // Single thread for ordering-critical consensus operations
        this.consensusExecutor = Executors.newSingleThreadExecutor(r -> {
            var thread = new Thread(r, "consensus-executor");
            thread.setDaemon(true);
            return thread;
        });
        
        // Work-stealing pool for parallel operations that don't require strict ordering
        this.fastPathExecutor = ForkJoinPool.commonPool();
        
        startTaskProcessor();
    }
    
    private void startTaskProcessor() {
        consensusExecutor.submit(() -> {
            while (!shutdown.get()) {
                try {
                    // Process critical tasks first
                    var criticalTask = criticalTasks.poll(10, TimeUnit.MILLISECONDS);
                    if (criticalTask != null) {
                        criticalTask.run();
                        continue;
                    }
                    
                    // Then process fast path tasks
                    var fastTask = fastPathTasks.poll(10, TimeUnit.MILLISECONDS);
                    if (fastTask != null) {
                        fastTask.run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error processing consensus task", e);
                }
            }
        });
    }
    
    /**
     * Execute ordering-critical consensus operations (voting, decisions)
     */
    public void executeCritical(Runnable task) {
        if (shutdown.get()) {
            return;
        }
        criticalTasks.offer(new ConsensusTask(task, TaskType.CRITICAL));
    }
    
    /**
     * Execute operations that can run in parallel (proposals, sync requests)
     */
    public void executeFastPath(Runnable task) {
        if (shutdown.get()) {
            return;
        }
        fastPathExecutor.submit(task);
    }
    
    /**
     * Execute non-critical tasks that can be delayed
     */
    public void executeDeferred(Runnable task) {
        if (shutdown.get()) {
            return;
        }
        fastPathTasks.offer(new ConsensusTask(task, TaskType.DEFERRED));
    }
    
    public void shutdown() {
        shutdown.set(true);
        consensusExecutor.shutdown();
        try {
            if (!consensusExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                consensusExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            consensusExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private static class ConsensusTask implements Runnable {
        private final Runnable task;
        private final TaskType type;
        
        ConsensusTask(Runnable task, TaskType type) {
            this.task = task;
            this.type = type;
        }
        
        @Override
        public void run() {
            task.run();
        }
    }
    
    private enum TaskType {
        CRITICAL,    // Must be executed in order
        DEFERRED     // Can be delayed
    }
}