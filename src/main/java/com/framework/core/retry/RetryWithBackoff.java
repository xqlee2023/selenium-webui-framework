package com.framework.core.retry;

import com.framework.utils.LogUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Sophisticated retry mechanism with exponential backoff.
 * Supports:
 * - Configurable max attempts
 * - Exponential backoff (with jitter)
 * - Conditional retry (only retry on specific exceptions)
 * - Timeout
 */
public class RetryWithBackoff {

    private final int maxAttempts;
    private final long initialDelayMs;
    private final double multiplier;
    private final long maxDelayMs;
    private final Predicate<Throwable> retryCondition;

    private RetryWithBackoff(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.initialDelayMs = builder.initialDelayMs;
        this.multiplier = builder.multiplier;
        this.maxDelayMs = builder.maxDelayMs;
        this.retryCondition = builder.retryCondition;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Execute with retry.
     *
     * @param action Action to execute
     * @param <T>    Return type
     * @return Result of the action
     */
    public <T> T execute(Supplier<T> action) {
        int attempt = 0;
        long delay = initialDelayMs;
        Throwable lastException = null;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                LogUtils.debug(getClass(), "🔄 Attempt {}/{}", attempt, maxAttempts);
                return action.get();
            } catch (Throwable e) {
                lastException = e;

                if (attempt >= maxAttempts) {
                    LogUtils.error(getClass(), "❌ All {} attempts failed", maxAttempts);
                    break;
                }

                if (!retryCondition.test(e)) {
                    LogUtils.warn(getClass(), "⏭️ Exception not retryable: {}", e.getMessage());
                    break;
                }

                // Add jitter: ±20%
                long jitter = (long) (delay * (0.8 + Math.random() * 0.4));
                LogUtils.warn(getClass(), "🔄 Retry {}/{} after {}ms | error: {}",
                        attempt, maxAttempts, jitter, e.getMessage());

                try {
                    TimeUnit.MILLISECONDS.sleep(jitter);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }

                delay = Math.min((long) (delay * multiplier), maxDelayMs);
            }
        }

        if (lastException instanceof RuntimeException) {
            throw (RuntimeException) lastException;
        }
        throw new RuntimeException("Operation failed after " + maxAttempts + " attempts", lastException);
    }

    /**
     * Execute a void action with retry.
     */
    public void executeVoid(Runnable action) {
        execute(() -> {
            action.run();
            return null;
        });
    }

    // ========== Builder ==========

    public static class Builder {
        private int maxAttempts = 3;
        private long initialDelayMs = 1000;
        private double multiplier = 2.0;
        private long maxDelayMs = 30000;
        private Predicate<Throwable> retryCondition = e -> true;

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder initialDelay(long delay, TimeUnit unit) {
            this.initialDelayMs = unit.toMillis(delay);
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder maxDelay(long delay, TimeUnit unit) {
            this.maxDelayMs = unit.toMillis(delay);
            return this;
        }

        public Builder retryOn(Class<? extends Throwable>... exceptions) {
            this.retryCondition = e -> {
                for (Class<? extends Throwable> ex : exceptions) {
                    if (ex.isInstance(e)) return true;
                }
                return false;
            };
            return this;
        }

        public Builder retryOnStaleElement() {
            return retryOn(org.openqa.selenium.StaleElementReferenceException.class,
                    org.openqa.selenium.ElementClickInterceptedException.class,
                    org.openqa.selenium.TimeoutException.class);
        }

        public RetryWithBackoff build() {
            return new RetryWithBackoff(this);
        }
    }

    // ========== Common Presets ==========

    /** Quick retry for flaky elements: 3 attempts, 500ms initial, 2x backoff. */
    public static RetryWithBackoff quick() {
        return builder()
                .maxAttempts(3)
                .initialDelay(500, TimeUnit.MILLISECONDS)
                .multiplier(2.0)
                .retryOnStaleElement()
                .build();
    }

    /** Heavy retry for unstable operations: 5 attempts, 2s initial, 3x backoff. */
    public static RetryWithBackoff heavy() {
        return builder()
                .maxAttempts(5)
                .initialDelay(2, TimeUnit.SECONDS)
                .multiplier(3.0)
                .maxDelay(60, TimeUnit.SECONDS)
                .retryOnStaleElement()
                .build();
    }
}
