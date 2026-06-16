package com.framework.annotations;

import java.lang.annotation.*;

/**
 * Conditionally skip a test based on runtime conditions.
 *
 * Usage:
 *   @SkipIf(browser = "safari", reason = "Safari not supported for this feature")
 *   @SkipIf(os = "windows", reason = "Only macOS/Linux")
 *   @SkipIf(env = "prod", reason = "Destructive test")
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(SkipIf.List.class)
public @interface SkipIf {

    /** Skip if browser matches (comma-separated: "chrome,firefox"). */
    String browser() default "";

    /** Skip if OS matches (comma-separated: "mac,windows,linux"). */
    String os() default "";

    /** Skip if environment matches ("dev,qa,prod"). */
    String env() default "";

    /** Skip if feature flag/enabled. */
    String featureFlag() default "";

    /** Reason for skipping. */
    String reason() default "";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface List {
        SkipIf[] value();
    }
}
