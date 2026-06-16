package com.framework.core.listener;

import com.framework.core.ConfigManager;
import com.framework.core.hook.*;
import com.framework.event.EventBus;
import com.framework.event.TestEvent;
import com.framework.event.SuiteEvent;
import com.framework.utils.LogUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import org.testng.ITestResult;
import com.framework.notification.NotificationHook;

/**
 * ===========================================
 * 🔌 Hook 注册中心 — 将传统 Hook 适配到 EventBus
 * ===========================================
 *
 * 负责在框架启动时，将现有的 Hook 实现注册到 EventBus。
 * 通过 lambda 适配器将 Hook 的旧接口映射到新的事件类型。
 *
 * 新增分析维度 = 在此类注册一行新的 EventBus 订阅。
 */
public final class HookRegistration {

    private static final AtomicBoolean registered = new AtomicBoolean(false);

    private HookRegistration() {}

    /**
     * 注册所有启用的 Hook 到 EventBus。
     * 在 BaseTest @BeforeSuite 中调用。
     */
    public static void registerAll() {
        if (!registered.compareAndSet(false, true)) return;

        ConfigManager cfg = ConfigManager.get();

        // ===== 基础 Hook：始终启用 =====
        HistoryRecorderHook historyHook = new HistoryRecorderHook();
        AllureEnvironmentHook allureHook = new AllureEnvironmentHook();

        registerTestListenerHooks(historyHook, allureHook);

        // ===== 失败截图：按配置启用 =====
        if (cfg.screenshotOnFailure()) {
            ScreenshotHook screenshotHook = new ScreenshotHook();
            registerTestListenerHooks(screenshotHook);
            LogUtils.info(HookRegistration.class, "失败截图已启用 (screenshotOnFailure=true)");
        } else {
            LogUtils.info(HookRegistration.class, "失败截图已禁用 (screenshotOnFailure=false)");
        }

        // ===== 无障碍检测 =====
        if (cfg.accessibilityEnabled()) {
            AccessibilityHook a11yHook = new AccessibilityHook();
            registerTestListenerHooks(a11yHook);
        }

        // ===== 通知：按配置启用 =====
        try {
            var notifCfg = com.framework.notification.NotificationConfig.load();
            if (notifCfg != null && notifCfg.enabled) {
                NotificationHook notificationHook = new NotificationHook();
                registerTestListenerHooks(notificationHook);
                LogUtils.info(HookRegistration.class, "钉钉通知已启用 (notification.enabled=true)");
            } else {
                LogUtils.info(HookRegistration.class, "钉钉通知已禁用 (notification.enabled=false)");
            }
        } catch (Exception e) {
            LogUtils.debug(HookRegistration.class, "通知配置加载失败，跳过通知注册: {}", e.getMessage());
        }

        // ===== AI Hook：按配置启用 =====
        if (cfg.aiEnabled()) {
            AIFailureAnalysisHook aiFailureHook = new AIFailureAnalysisHook();
            AIReportHook aiReportHook = new AIReportHook();
            FlakyDetectionHook flakyHook = new FlakyDetectionHook();

            registerTestListenerHooks(aiFailureHook, aiReportHook, flakyHook);
        }

        LogUtils.info(HookRegistration.class,
                "Hook 注册完成，已注册 {} 个组件", countHooks());
    }

    /**
     * 将传统的 TestEventHook 实现注册到 EventBus。
     * 适配方法：将 TestNG 事件类型转换为 EventBus 事件类型。
     */
    private static void registerTestListenerHooks(TestEventHook... hooks) {
        for (TestEventHook hook : hooks) {
            String name = hook.getClass().getSimpleName();

            // Suite 事件
            if (hasMethod(hook, "onSuiteStart")) {
                EventBus.register(SuiteEvent.Started.class, event ->
                        hook.onSuiteStart(event.getSuite()), hook);
            }
            if (hasMethod(hook, "onSuiteFinish")) {
                EventBus.register(SuiteEvent.Finished.class, event ->
                        hook.onSuiteFinish(event.getSuite()), hook);
            }

            // 测试事件
            if (hasMethod(hook, "onTestStart")) {
                EventBus.register(TestEvent.Started.class, event ->
                        hook.onTestStart(event.getResult()), hook);
            }
            if (hasMethod(hook, "onTestSuccess")) {
                EventBus.register(TestEvent.Success.class, event ->
                        hook.onTestSuccess(event.getResult()), hook);
            }
            if (hasMethod(hook, "onTestFailure")) {
                EventBus.register(TestEvent.Failure.class, event ->
                        hook.onTestFailure(event.getResult()), hook);
            }
            if (hasMethod(hook, "onTestSkipped")) {
                EventBus.register(TestEvent.Skipped.class, event ->
                        hook.onTestSkipped(event.getResult()), hook);
            }

            LogUtils.debug(HookRegistration.class, "  注册 Hook: {}", name);
        }
    }

    private static boolean hasMethod(Object obj, String methodName) {
        try {
            obj.getClass().getMethod(methodName, determineParamType(methodName));
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Class<?> determineParamType(String methodName) {
        if (methodName.startsWith("onSuite")) return org.testng.ISuite.class;
        return ITestResult.class;
    }

    private static int countHooks() {
        // EventBus 内部维护，这里简化返回
        return -1;
    }

    /**
     * 重置（主要用于测试）。
     */
    public static void reset() {
        registered.set(false);
    }
}
