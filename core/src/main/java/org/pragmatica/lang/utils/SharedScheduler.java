package org.pragmatica.lang.utils;

import org.pragmatica.lang.io.TimeSpan;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class SharedScheduler {
    private SharedScheduler() {
    }

    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(2);

    public static void schedule(Runnable runnable, TimeSpan interval) {
        SCHEDULER.schedule(runnable, interval.millis(), TimeUnit.MILLISECONDS);
    }
}
