package com.ticket.system.auth.ratelimit;


import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

// One bucket per key (for example per IP). A request takes 1 token. Tokens refill steadily.
public class TokenBucketLimiter {

    private static final class Bucket {
        double tokens;
        long lastRefillNanos;
        Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.lastRefillNanos = now;
        }
    }

    private final int capacity;
    private final double refillPerNano;
    private final LongSupplier nanoClock;   // injected, so tests don't need to sleep
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketLimiter(int capacity, Duration refillPeriod, LongSupplier nanoClock) {
        this.capacity = capacity;
        this.refillPerNano = (double) capacity / refillPeriod.toNanos();
        this.nanoClock = nanoClock;
    }

    public Decision tryAcquire(String key) {
        System.out.println("Key<IP>: "+key);
        long now = nanoClock.getAsLong();
        System.out.println("Abhi: Now"+now);
        cleanupIfLarge(now);
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, now));
        System.out.println("Capacity: "+capacity);
        synchronized (bucket) {
            // 1. add the tokens earned since the last request
            double earned = (now - bucket.lastRefillNanos) * refillPerNano;
            System.out.println("Earned: "+earned);
            bucket.tokens = Math.min(capacity, bucket.tokens + earned);
            System.out.println("Tokens: "+bucket.tokens);
            bucket.lastRefillNanos = now;
            System.out.println("LastRefillNanos: "+bucket.lastRefillNanos);

            // 2. take one token if there is one
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                System.out.println("Tokens remaining after usage (>=1>): "+bucket.tokens);
                return new Decision(true, 0);
            }
            // 3. otherwise say how long until one token is back
            double missing = 1 - bucket.tokens;
            System.out.println("Missing: "+missing);
            long waitSeconds = (long) Math.ceil(missing / refillPerNano / 1_000_000_000d);
            System.out.println("Wait Seconds: "+waitSeconds);
            return new Decision(false, Math.max(1, waitSeconds));
        }
    }

    // Keeps memory bounded: drop buckets that have been idle long enough to be full again
    private void cleanupIfLarge(long now) {
        if (buckets.size() < 10_000) return;
        long idleNanos = (long) (capacity / refillPerNano);
        buckets.values().removeIf(b -> now - b.lastRefillNanos > idleNanos);
    }
}
