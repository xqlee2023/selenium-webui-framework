package com.framework.utils.data;

import java.lang.annotation.*;

/**
 * Annotation for test methods to specify data file source.
 * Used with DataProviderHelper.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface DataFile {
    /** Path to the data file. */
    String path();

    /** Sheet name (Excel) or JSON key path (e.g., "users.valid"). */
    String sheet() default "";

    /** JSON/YAML key path for nested data extraction (dot-separated). */
    String key() default "";
}
