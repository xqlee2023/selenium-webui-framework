package com.framework.annotations;

import java.lang.annotation.*;

/**
 * Rich test case metadata annotation.
 * Provides detailed information about test cases for reporting and traceability.
 *
 * Usage:
 *   @TestCaseInfo(
 *       id = "TC-001",
 *       module = "Login",
 *       feature = "Authentication",
 *       author = "QingGe",
 *       priority = Priority.CRITICAL,
 *       requirement = "REQ-LOGIN-001",
 *       jiraId = "PROJ-1234"
 *   )
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface TestCaseInfo {

    /** Unique test case ID. */
    String id() default "";

    /** Module name. */
    String module() default "";

    /** Feature name. */
    String feature() default "";

    /** Test author. */
    String author() default "";

    /** Priority level. */
    Priority priority() default Priority.NORMAL;

    /** Business requirement ID. */
    String requirement() default "";

    /** JIRA / issue tracking ID. */
    String jiraId() default "";

    /** Test case category. */
    String category() default "";

    /** Additional labels. */
    String[] labels() default {};

    enum Priority {
        BLOCKER,
        CRITICAL,
        MAJOR,
        NORMAL,
        MINOR,
        TRIVIAL
    }
}
