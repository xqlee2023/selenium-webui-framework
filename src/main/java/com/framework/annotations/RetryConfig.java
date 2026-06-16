package com.framework.annotations;

import java.lang.annotation.*;

/**
 * Per-test retry configuration override.
 * Overrides global retry settings from config.yaml.
 *
 * Usage:
 *   @RetryConfig(maxRetries = 3, retryOn = "StaleElementReferenceException")
 *   @Test
 *   public void flakyTest() { ... }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface RetryConfig {

    /** Maximum retries for this specific test. */
    int maxRetries() default 1;

    /** Exception types to retry on (FQN class names). */
    String[] retryOn() default {};

    /** Backoff delay in ms between retries. */
    long delayMs() default 1000;

    /** Whether to retry (override global). */
    boolean enabled() default true;
}
