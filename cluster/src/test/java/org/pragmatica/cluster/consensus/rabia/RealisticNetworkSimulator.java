package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.net.local.LocalNetwork;
import org.pragmatica.lang.io.TimeSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Realistic network simulator that models real-world network conditions
 * for testing consensus performance under various scenarios.
 */
public class RealisticNetworkSimulator {
    private static final Logger log = LoggerFactory.getLogger(RealisticNetworkSimulator.class);
    
    /**
     * Network profiles based on real-world scenarios
     */
    public enum NetworkProfile {
        // Data center networks (low latency, high reliability)
        DATACENTER_LAN(
            "Datacenter LAN",
            1, 3,        // 1-3ms latency
            0.001        // 0.1% packet loss
        ),
        
        // Cloud provider networks (moderate latency, good reliability)
        CLOUD_PROVIDER(
            "Cloud Provider",
            5, 15,       // 5-15ms latency
            0.005        // 0.5% packet loss
        ),
        
        // Regional WAN (moderate latency, occasional issues)
        REGIONAL_WAN(
            "Regional WAN",
            20, 50,      // 20-50ms latency
            0.01         // 1% packet loss
        ),
        
        // Cross-continental (high latency, reliability issues)
        CROSS_CONTINENTAL(
            "Cross-Continental",
            100, 200,    // 100-200ms latency
            0.02         // 2% packet loss
        ),
        
        // Satellite connection (very high latency, reliability issues)
        SATELLITE(
            "Satellite",
            500, 700,    // 500-700ms latency
            0.05         // 5% packet loss
        ),
        
        // Mobile/WiFi (variable latency, burst losses)
        MOBILE_WIFI(
            "Mobile/WiFi",
            30, 100,     // 30-100ms latency
            0.03         // 3% packet loss
        ),
        
        // Degraded network (simulating network issues)
        DEGRADED(
            "Degraded Network",
            50, 150,     // 50-150ms latency
            0.08         // 8% packet loss
        );
        
        private final String description;
        private final int minLatencyMs;
        private final int maxLatencyMs;
        private final double packetLossRate;
        
        NetworkProfile(String description, int minLatencyMs, int maxLatencyMs, double packetLossRate) {
            this.description = description;
            this.minLatencyMs = minLatencyMs;
            this.maxLatencyMs = maxLatencyMs;
            this.packetLossRate = packetLossRate;
        }
        
        public String getDescription() {
            return description;
        }
        
        public TimeSpan getRandomLatency() {
            int latency = ThreadLocalRandom.current().nextInt(minLatencyMs, maxLatencyMs + 1);
            return TimeSpan.timeSpan(latency).millis();
        }
        
        public double getPacketLossRate() {
            return packetLossRate;
        }
        
        public int getAvgLatencyMs() {
            return (minLatencyMs + maxLatencyMs) / 2;
        }
    }
    
    /**
     * Enhanced fault injector that simulates realistic network behavior
     */
    public static class RealisticFaultInjector extends LocalNetwork.FaultInjector {
        private NetworkProfile currentProfile;
        private boolean isVariableLatency = true;
        private boolean isBurstLoss = false;
        private int burstLossCounter = 0;
        private int burstLossLength = 0;
        
        public RealisticFaultInjector() {
            super();
            setProfile(NetworkProfile.DATACENTER_LAN);
        }
        
        public void setProfile(NetworkProfile profile) {
            this.currentProfile = profile;
            
            // Configure base network conditions
            clearAllFaults();
            setFault(LocalNetwork.FaultType.MESSAGE_DELAY, true);
            setFault(LocalNetwork.FaultType.MESSAGE_LOSS, true);
            setMessageLossRate(profile.getPacketLossRate());
            
            log.info("Network profile set to: {} (avg {}ms latency, {:.1f}% loss)", 
                    profile.getDescription(), 
                    profile.getAvgLatencyMs(), 
                    profile.getPacketLossRate() * 100);
        }
        
        public void enableVariableLatency(boolean enable) {
            this.isVariableLatency = enable;
        }
        
        public void enableBurstLoss(boolean enable) {
            this.isBurstLoss = enable;
        }
        
        @Override
        public boolean shouldDelayMessage() {
            return true; // Always apply latency simulation
        }
        
        @Override
        public TimeSpan messageDelay() {
            if (isVariableLatency) {
                return currentProfile.getRandomLatency();
            } else {
                return TimeSpan.timeSpan(currentProfile.getAvgLatencyMs()).millis();
            }
        }
        
        @Override
        public boolean shouldDropMessage() {
            if (isBurstLoss) {
                return shouldDropMessageWithBursts();
            } else {
                return super.shouldDropMessage();
            }
        }
        
        private boolean shouldDropMessageWithBursts() {
            // Simulate burst packet loss (common in real networks)
            if (burstLossCounter > 0) {
                burstLossCounter--;
                return true;
            }
            
            // Start a new burst loss period occasionally
            if (ThreadLocalRandom.current().nextDouble() < currentProfile.getPacketLossRate() / 2) {
                burstLossLength = ThreadLocalRandom.current().nextInt(2, 6); // 2-5 messages
                burstLossCounter = burstLossLength;
                return true;
            }
            
            return false;
        }
        
        /**
         * Simulate network congestion events
         */
        public void simulateCongestion(int durationMs, double lossMultiplier) {
            var originalLoss = currentProfile.getPacketLossRate();
            setMessageLossRate(Math.min(0.5, originalLoss * lossMultiplier));
            
            // Schedule return to normal
            new Thread(() -> {
                try {
                    Thread.sleep(durationMs);
                    setMessageLossRate(originalLoss);
                    log.info("Network congestion cleared");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            log.info("Simulating network congestion for {}ms (loss rate: {:.1f}%)", 
                    durationMs, getPacketLossRate() * 100);
        }
        
        /**
         * Simulate gradual network degradation
         */
        public void simulateGradualDegradation(NetworkProfile targetProfile, int transitionMs) {
            var startProfile = currentProfile;
            var steps = 10;
            var stepMs = transitionMs / steps;
            
            new Thread(() -> {
                for (int step = 1; step <= steps; step++) {
                    try {
                        Thread.sleep(stepMs);
                        
                        // Interpolate between profiles
                        double ratio = (double) step / steps;
                        double newLoss = startProfile.getPacketLossRate() + 
                                        ratio * (targetProfile.getPacketLossRate() - startProfile.getPacketLossRate());
                        
                        setMessageLossRate(newLoss);
                        
                        if (step == steps) {
                            setProfile(targetProfile);
                            log.info("Network degradation complete");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
            
            log.info("Starting gradual network degradation from {} to {} over {}ms", 
                    startProfile.getDescription(), targetProfile.getDescription(), transitionMs);
        }
        
        /**
         * Get current network statistics
         */
        public NetworkStats getStats() {
            return new NetworkStats(
                currentProfile.getDescription(),
                currentProfile.getAvgLatencyMs(),
                currentProfile.getPacketLossRate() * 100,
                isVariableLatency,
                isBurstLoss
            );
        }
    }
    
    /**
     * Network statistics for monitoring
     */
    public record NetworkStats(
        String profileName,
        int avgLatencyMs,
        double packetLossPercent,
        boolean variableLatency,
        boolean burstLoss
    ) {
        @Override
        public String toString() {
            return String.format("%s: %dms latency, %.1f%% loss%s%s",
                    profileName,
                    avgLatencyMs,
                    packetLossPercent,
                    variableLatency ? ", variable latency" : "",
                    burstLoss ? ", burst loss" : "");
        }
    }
    
    /**
     * Predefined test scenarios
     */
    public static class TestScenarios {
        
        /**
         * Optimal conditions for baseline measurement
         */
        public static void setupOptimal(RealisticFaultInjector injector) {
            injector.setProfile(NetworkProfile.DATACENTER_LAN);
            injector.enableVariableLatency(false);
            injector.enableBurstLoss(false);
        }
        
        /**
         * Typical cloud deployment
         */
        public static void setupCloudDeployment(RealisticFaultInjector injector) {
            injector.setProfile(NetworkProfile.CLOUD_PROVIDER);
            injector.enableVariableLatency(true);
            injector.enableBurstLoss(false);
        }
        
        /**
         * Geo-distributed deployment
         */
        public static void setupGeoDistributed(RealisticFaultInjector injector) {
            injector.setProfile(NetworkProfile.REGIONAL_WAN);
            injector.enableVariableLatency(true);
            injector.enableBurstLoss(true);
        }
        
        /**
         * Adverse network conditions
         */
        public static void setupAdverse(RealisticFaultInjector injector) {
            injector.setProfile(NetworkProfile.DEGRADED);
            injector.enableVariableLatency(true);
            injector.enableBurstLoss(true);
        }
        
        /**
         * Extreme conditions (satellite/very poor network)
         */
        public static void setupExtreme(RealisticFaultInjector injector) {
            injector.setProfile(NetworkProfile.SATELLITE);
            injector.enableVariableLatency(true);
            injector.enableBurstLoss(true);
        }
        
        /**
         * Mobile/edge deployment conditions
         */
        public static void setupMobile(RealisticFaultInjector injector) {
            injector.setProfile(NetworkProfile.MOBILE_WIFI);
            injector.enableVariableLatency(true);
            injector.enableBurstLoss(true);
        }
    }
    
    /**
     * Create a realistic fault injector for testing
     */
    public static RealisticFaultInjector createRealisticInjector(NetworkProfile profile) {
        var injector = new RealisticFaultInjector();
        injector.setProfile(profile);
        return injector;
    }
    
    /**
     * Create fault injector with custom conditions
     */
    public static RealisticFaultInjector createCustomInjector(
            int minLatencyMs, 
            int maxLatencyMs, 
            double packetLossRate,
            boolean variableLatency,
            boolean burstLoss) {
        
        var customProfile = new NetworkProfile("Custom", minLatencyMs, maxLatencyMs, packetLossRate) {
            // Anonymous implementation for custom profiles
        };
        
        var injector = new RealisticFaultInjector();
        injector.setProfile(customProfile);
        injector.enableVariableLatency(variableLatency);
        injector.enableBurstLoss(burstLoss);
        
        return injector;
    }
}