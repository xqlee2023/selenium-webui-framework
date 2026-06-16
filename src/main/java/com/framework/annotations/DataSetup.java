package com.framework.annotations;

import java.lang.annotation.*;

/**
 * Marks a method as a test data setup/teardown step for Allure reporting.
 * Used for API or database operations that prepare data before a test.
 *
 * Usage:
 *   @DataSetup("Create test user via API")
 *   public void setupUser() { ... }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface DataSetup {
    String value() default "";
}
