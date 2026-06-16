package com.framework.core.listener;

import com.framework.core.ConfigManager;
import com.framework.event.EventBus;
import com.framework.event.TestEvent;
import com.framework.event.SuiteEvent;
import com.framework.utils.LogUtils;
import org.testng.*;

/**
 * ===========================================
 * 👂 TestNG 监听器 — 事件分发器（EventBus 版）
 * ===========================================
 *
 * 遵循 OCP + SRP：只做 TestNG 事件到 EventBus 的翻译。
 * 所有业务逻辑（截图、AI诊断、Flaky检测、报告、历史）通过事件订阅实现。
 *
 * 新增分析维度 = 新建 Hook 类 + 注册 EventBus 订阅。
 *
 * v3.2.0 重构：从硬编码 Hook 列表迁移到 EventBus 事件驱动架构。
 */
public class TestListener implements ITestListener, ISuiteListener {

    public TestListener() {
        // 初始化配置（确保在 Hook 注册前就绪）
        ConfigManager.init();

        LogUtils.info(getClass(), "事件驱动架构已启用: TestNG → EventBus → Hook");
    }

    // ========== ISuiteListener ==========

    @Override
    public void onStart(ISuite suite) {
        LogUtils.info(getClass(), "═══════════════════════════════════════");
        LogUtils.info(getClass(), "Suite: {} | Browser: {} | Mode: {} | Env: {}",
                suite.getName(),
                ConfigManager.get().browser(),
                ConfigManager.get().executionMode(),
                ConfigManager.get().environment());
        LogUtils.info(getClass(), "═══════════════════════════════════════");
        EventBus.post(new SuiteEvent.Started(suite));
    }

    @Override
    public void onFinish(ISuite suite) {
        EventBus.post(new SuiteEvent.Finished(suite));
        LogUtils.info(getClass(), "Suite finished: {}", suite.getName());
    }

    // ========== ITestListener ==========

    @Override
    public void onTestStart(ITestResult result) {
        LogUtils.info(getClass(), "{}", result.getName());
        EventBus.post(new TestEvent.Started(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        LogUtils.info(getClass(), "{} ({}ms)", result.getName(), duration);
        EventBus.post(new TestEvent.Success(result));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        LogUtils.error(getClass(), "{} ({}ms)", result.getName(), duration);
        Throwable t = result.getThrowable();
        if (t != null) {
            LogUtils.error(getClass(), "{}: {}", t.getClass().getSimpleName(), t.getMessage());
        }
        EventBus.post(new TestEvent.Failure(result));
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LogUtils.warn(getClass(), "{}", result.getName());
        EventBus.post(new TestEvent.Skipped(result));
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        LogUtils.warn(getClass(), "{} (partial)", result.getName());
    }
}
