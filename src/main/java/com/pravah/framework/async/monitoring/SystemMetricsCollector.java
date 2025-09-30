package com.pravah.framework.async.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Collects system-level metrics for the async processing framework.
 * 
 * <p>This component periodically collects JVM and system metrics
 * to provide insights into resource utilization and performance.
 * 
 * @author Ankur Rai
 * @version 1.0
 */
public class SystemMetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(SystemMetricsCollector.class);

    private final MetricsCollector metricsCollector;
    private final AsyncFrameworkMetricsAutoConfiguration.MonitoringProperties properties;
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;

    public SystemMetricsCollector(
            MetricsCollector metricsCollector,
            AsyncFrameworkMetricsAutoConfiguration.MonitoringProperties properties) {
        this.metricsCollector = metricsCollector;
        this.properties = properties;
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        
        logger.info("SystemMetricsCollector initialized with collection interval: {}", 
            properties.getCollectionInterval());
    }

    /**
     * Collects JVM memory metrics.
     */
    @Scheduled(fixedRateString = "#{@monitoringProperties.collectionInterval.toMillis()}")
    public void collectMemoryMetrics() {
        if (!properties.isJvmMetrics()) {
            return;
        }

        try {
            long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryMXBean.getHeapMemoryUsage().getMax();
            long committedMemory = memoryMXBean.getHeapMemoryUsage().getCommitted();
            
            metricsCollector.recordMemoryUsage(usedMemory, maxMemory);
            
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".memory.committed", 
                committedMemory, 
                Map.of("type", "heap")
            );
            
            // Non-heap memory
            long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryMXBean.getNonHeapMemoryUsage().getMax();
            
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".memory.nonheap.used", 
                nonHeapUsed, 
                Map.of("type", "nonheap")
            );
            
            if (nonHeapMax > 0) {
                metricsCollector.recordGauge(
                    properties.getMetricPrefix() + ".memory.nonheap.max", 
                    nonHeapMax, 
                    Map.of("type", "nonheap")
                );
            }
            
            logger.debug("Collected memory metrics - Heap used: {} MB, Max: {} MB", 
                usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
                
        } catch (Exception e) {
            logger.warn("Error collecting memory metrics", e);
        }
    }

    /**
     * Collects JVM thread metrics.
     */
    @Scheduled(fixedRateString = "#{@monitoringProperties.collectionInterval.toMillis()}")
    public void collectThreadMetrics() {
        if (!properties.isJvmMetrics()) {
            return;
        }

        try {
            int threadCount = threadMXBean.getThreadCount();
            int daemonThreadCount = threadMXBean.getDaemonThreadCount();
            long totalStartedThreadCount = threadMXBean.getTotalStartedThreadCount();
            
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".threads.count", 
                threadCount, 
                Map.of("type", "total")
            );
            
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".threads.daemon", 
                daemonThreadCount, 
                Map.of("type", "daemon")
            );
            
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".threads.started.total", 
                totalStartedThreadCount, 
                Map.of("type", "started")
            );
            
            logger.debug("Collected thread metrics - Active: {}, Daemon: {}, Total started: {}", 
                threadCount, daemonThreadCount, totalStartedThreadCount);
                
        } catch (Exception e) {
            logger.warn("Error collecting thread metrics", e);
        }
    }

    /**
     * Collects GC metrics.
     */
    @Scheduled(fixedRateString = "#{@monitoringProperties.collectionInterval.toMillis()}")
    public void collectGarbageCollectionMetrics() {
        if (!properties.isJvmMetrics()) {
            return;
        }

        try {
            ManagementFactory.getGarbageCollectorMXBeans().forEach(gcBean -> {
                String gcName = gcBean.getName().replaceAll("\\s+", "_").toLowerCase();
                long collectionCount = gcBean.getCollectionCount();
                long collectionTime = gcBean.getCollectionTime();
                
                if (collectionCount >= 0) {
                    metricsCollector.recordGauge(
                        properties.getMetricPrefix() + ".gc.collections", 
                        collectionCount, 
                        Map.of("gc", gcName)
                    );
                }
                
                if (collectionTime >= 0) {
                    metricsCollector.recordGauge(
                        properties.getMetricPrefix() + ".gc.time", 
                        collectionTime, 
                        Map.of("gc", gcName)
                    );
                }
            });
            
            logger.debug("Collected GC metrics");
            
        } catch (Exception e) {
            logger.warn("Error collecting GC metrics", e);
        }
    }

    /**
     * Collects class loading metrics.
     */
    @Scheduled(fixedRateString = "#{@monitoringProperties.collectionInterval.toMillis()}")
    public void collectClassLoadingMetrics() {
        if (!properties.isJvmMetrics()) {
            return;
        }

        try {
            var classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
            
            int loadedClassCount = classLoadingMXBean.getLoadedClassCount();
            long totalLoadedClassCount = classLoadingMXBean.getTotalLoadedClassCount();
            long unloadedClassCount = classLoadingMXBean.getUnloadedClassCount();
            
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".classes.loaded", 
                loadedClassCount, 
                Map.of()
            );
            
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".classes.loaded.total", 
                totalLoadedClassCount, 
                Map.of()
            );
            
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".classes.unloaded", 
                unloadedClassCount, 
                Map.of()
            );
            
            logger.debug("Collected class loading metrics - Loaded: {}, Total: {}, Unloaded: {}", 
                loadedClassCount, totalLoadedClassCount, unloadedClassCount);
                
        } catch (Exception e) {
            logger.warn("Error collecting class loading metrics", e);
        }
    }

    /**
     * Collects CPU metrics.
     */
    @Scheduled(fixedRateString = "#{@monitoringProperties.collectionInterval.toMillis()}")
    public void collectCpuMetrics() {
        if (!properties.isSystemMetrics()) {
            return;
        }

        try {
            var operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
            
            double systemLoadAverage = operatingSystemMXBean.getSystemLoadAverage();
            int availableProcessors = operatingSystemMXBean.getAvailableProcessors();
            
            if (systemLoadAverage >= 0) {
                metricsCollector.recordGauge(
                    properties.getMetricPrefix() + ".system.load.average", 
                    systemLoadAverage, 
                    Map.of()
                );
            }
            
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".system.processors", 
                availableProcessors, 
                Map.of()
            );
            
            // Try to get process CPU load if available
            if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean osBean) {
                double processCpuLoad = osBean.getProcessCpuLoad();
                double systemCpuLoad = osBean.getSystemCpuLoad();
                
                if (processCpuLoad >= 0) {
                    metricsCollector.recordGauge(
                        properties.getMetricPrefix() + ".process.cpu.usage", 
                        processCpuLoad * 100, 
                        Map.of("unit", "percent")
                    );
                }
                
                if (systemCpuLoad >= 0) {
                    metricsCollector.recordGauge(
                        properties.getMetricPrefix() + ".system.cpu.usage", 
                        systemCpuLoad * 100, 
                        Map.of("unit", "percent")
                    );
                }
            }
            
            logger.debug("Collected CPU metrics - Load average: {}, Processors: {}", 
                systemLoadAverage, availableProcessors);
                
        } catch (Exception e) {
            logger.warn("Error collecting CPU metrics", e);
        }
    }

    /**
     * Records thread pool metrics for a given executor.
     */
    public void recordThreadPoolMetrics(String poolName, ThreadPoolExecutor executor) {
        if (!properties.isSystemMetrics()) {
            return;
        }

        try {
            int activeCount = executor.getActiveCount();
            int queueSize = executor.getQueue().size();
            long completedTaskCount = executor.getCompletedTaskCount();
            int poolSize = executor.getPoolSize();
            int corePoolSize = executor.getCorePoolSize();
            int maximumPoolSize = executor.getMaximumPoolSize();
            
            metricsCollector.recordThreadPoolMetrics(poolName, activeCount, queueSize, completedTaskCount);
            
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".thread.pool.size", 
                poolSize, 
                Map.of("pool", poolName)
            );
            
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".thread.pool.core.size", 
                corePoolSize, 
                Map.of("pool", poolName)
            );
            
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".thread.pool.max.size", 
                maximumPoolSize, 
                Map.of("pool", poolName)
            );
            
            logger.debug("Recorded thread pool metrics for {}: active={}, queue={}, completed={}", 
                poolName, activeCount, queueSize, completedTaskCount);
                
        } catch (Exception e) {
            logger.warn("Error recording thread pool metrics for {}", poolName, e);
        }
    }
}