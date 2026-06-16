package com.framework.core.hook;

import org.testng.ISuite;
import org.testng.ITestResult;

/**
 * ===========================================
 * 🔗 测试事件 Hook 接口 — 观察者模式 + OCP
 * ===========================================
 *
 * 每个 Hook 只关心自己需要的事件，用 default 方法避免空实现。
 * TestListener 变成纯粹的事件分发器，业务逻辑在各自 Hook 中。
 *
 * 新增分析维度 = 新建 Hook 类 + 注册一行，TestListener 零改动。
 */
public interface TestEventHook {

    /** Suite 开始时调用 */
    default void onSuiteStart(ISuite suite) {}

    /** Suite 结束时调用 */
    default void onSuiteFinish(ISuite suite) {}

    /** 单个测试开始时调用 */
    default void onTestStart(ITestResult result) {}

    /** 测试成功时调用 */
    default void onTestSuccess(ITestResult result) {}

    /** 测试失败时调用 */
    default void onTestFailure(ITestResult result) {}

    /** 测试跳过时调用 */
    default void onTestSkipped(ITestResult result) {}
}
