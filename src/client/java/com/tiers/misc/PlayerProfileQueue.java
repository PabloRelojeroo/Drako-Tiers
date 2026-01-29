package com.tiers.misc;

import com.tiers.profiles.PlayerProfile;
import com.tiers.profiles.Status;

import java.util.concurrent.*;

public class PlayerProfileQueue {
    private static final ConcurrentLinkedDeque<PlayerProfile> queue = new ConcurrentLinkedDeque<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private static final int MAX_CONCURRENT = 10;
    private static int activeRequests = 0;
    private static final Object lock = new Object();

    static {
        scheduler.scheduleAtFixedRate(PlayerProfileQueue::processQueue, 0, 20, TimeUnit.MILLISECONDS);
    }

    public static void enqueue(PlayerProfile profile) {
        queue.add(profile);
    }

    private static void processQueue() {
        synchronized (lock) {
            while (activeRequests < MAX_CONCURRENT && !queue.isEmpty()) {
                PlayerProfile profile = queue.poll();
                if (profile != null && profile.status == Status.SEARCHING) {
                    activeRequests++;
                    CompletableFuture.runAsync(() -> {
                        try {
                            profile.buildRequest(profile.name);
                        } finally {
                            synchronized (lock) {
                                activeRequests--;
                            }
                        }
                    });
                }
            }
        }
    }

    public static void putFirstInQueue(PlayerProfile profile) {
        queue.remove(profile);
        queue.addFirst(profile);
    }

    public static void changeToFirstInQueue(PlayerProfile profile) {
        if (queue.contains(profile)) {
            queue.remove(profile);
            queue.addFirst(profile);
        }
    }

    public static void clearQueue() {
        queue.clear();
        synchronized (lock) {
            activeRequests = 0;
        }
    }
}
