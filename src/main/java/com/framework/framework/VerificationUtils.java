package com.framework.framework;

import com.framework.utils.LogUtils;
import org.testng.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ===========================================
 * ✅ 断言/校验工具 (VerificationUtils)
 * ===========================================
 *
 * 遵循 单一职责：只做断言和校验。
 * 支持：软断言（收集所有失败）、集合断言、文字断言。
 *
 * 使用方式：
 *   VerificationUtils.softAssertionsBegin();
 *   VerificationUtils.softAssertEquals(a, b, "message");
 *   VerificationUtils.softAssertionsAssertAll();
 */
public class VerificationUtils {

    private static final ThreadLocal<List<AssertionError>> softErrors =
            ThreadLocal.withInitial(ArrayList::new);

    private VerificationUtils() {}

    // ========== 软断言 ==========

    /** 开始收集软断言。 */
    public static void softAssertionsBegin() {
        softErrors.get().clear();
    }

    /** 断言所有软断言。有失败则抛出汇总异常。 */
    public static void softAssertionsAssertAll() {
        List<AssertionError> errors = softErrors.get();
        if (!errors.isEmpty()) {
            softErrors.get().clear();
            String msg = errors.stream()
                    .map(e -> e.getMessage() != null ? e.getMessage() : "Assertion failed")
                    .collect(Collectors.joining("\n  • ", "\n  ⚠️ 软断言失败:\n  • ", ""));
            throw new AssertionError(msg);
        }
    }

    public static void softAssertTrue(boolean condition, String message) {
        try { Assert.assertTrue(condition, message);
        } catch (AssertionError e) { softErrors.get().add(e); }
    }

    public static void softAssertEquals(Object actual, Object expected, String message) {
        try { Assert.assertEquals(actual, expected, message);
        } catch (AssertionError e) { softErrors.get().add(e); }
    }

    // ========== 集合断言 ==========

    public static void assertListContains(List<String> actual, String expected) {
        boolean found = actual.stream().anyMatch(item -> item.contains(expected));
        Assert.assertTrue(found, "期望 '" + expected + "' 在列表: " + actual);
    }

    public static void assertListNotEmpty(List<?> list) {
        Assert.assertFalse(list.isEmpty(), "期望非空列表");
    }

    // ========== 文字断言 ==========

    public static void assertTextContains(String actual, String expected) {
        Assert.assertTrue(actual.contains(expected),
                "期望 '" + actual + "' 包含 '" + expected + "'");
    }

    public static void assertTextMatches(String actual, String regex) {
        Assert.assertTrue(actual.matches(regex),
                "期望 '" + actual + "' 匹配 '" + regex + "'");
    }

    // ========== 数字断言 ==========

    public static void assertBetween(int actual, int min, int max) {
        Assert.assertTrue(actual >= min && actual <= max,
                "期望 " + actual + " 在 [" + min + ", " + max + "] 之间");
    }
}
