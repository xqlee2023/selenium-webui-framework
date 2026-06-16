package com.framework.framework;

import com.framework.core.ConfigManager;
import com.framework.browser.DriverManager;
import com.framework.framework.ScreenshotHelper;
import com.framework.element.actions.ElementActions;
import com.framework.utils.LogUtils;
import com.framework.wait.WaitStrategy;
import com.framework.wait.WaitStrategyFactory;
import com.framework.core.listener.HookRegistration;
import com.framework.data.lifecycle.DataLifecycleManager;
import com.framework.core.security.DataSanitizer;
import com.framework.core.retry.AdaptiveRetryAnalyzer;
import io.qameta.allure.Allure;
import org.testng.ITestContext;
import org.testng.annotations.*;

import java.lang.reflect.Method;

/**
 * ===========================================
 * 🧪 测试基类 (BaseTest)
 * ===========================================
 *
 * 遵循 模板方法模式：定义了测试执行骨架，子类只需实现业务逻辑。
 * ThreadLocal 保证每个线程有自己的状态，支持并行执行。
 *
 * 子类用法：
 *   public class LoginTest extends BaseTest {
 *       @Test
 *       public void testLogin() {
 *           actions().type(...).click(...);
 *       }
 *   }
 */
@Listeners(com.framework.core.listener.TestListener.class)
public abstract class BaseTest {

    /** ThreadLocal 保证并行安全 */
    private static final ThreadLocal<ElementActions> ACTIONS = new ThreadLocal<>();
    private static final ThreadLocal<ScreenshotHelper> SCREENSHOT = new ThreadLocal<>();
    private static final ThreadLocal<WaitStrategy> WAIT = new ThreadLocal<>();

    protected ConfigManager config;

    // ========== 通过方法获取（支持并行） ==========

    protected ElementActions actions()      { return ACTIONS.get(); }
    protected ScreenshotHelper screenshot() { return SCREENSHOT.get(); }
    protected WaitStrategy waitStrategy()   { return WAIT.get(); }

    // ========== BeforeSuite ==========

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite(ITestContext context) {
        config = ConfigManager.init();

        // 注册 EventBus Hook（首次初始化）
        HookRegistration.registerAll();

        // AI 生命周期由 BaseTest 统一管理，而非 ConfigManager
        com.framework.ai.lifecycle.AILifecycleManager.init(config.aiConfig());

        // 自适应重试历史重置
        AdaptiveRetryAnalyzer.resetHistory();

        // 数据生命周期管理器初始化
        DataLifecycleManager.getInstance();

        LogUtils.info(getClass(), "═══════════════════════════════════════");
        LogUtils.info(getClass(), "Test Suite: {}", context.getSuite().getName());
        LogUtils.info(getClass(), "OS: {} | Java: {}",
                System.getProperty("os.name"), System.getProperty("java.version"));
        LogUtils.info(getClass(), "═══════════════════════════════════════");
        Allure.suite(context.getSuite().getName());
    }

    // ========== BeforeMethod ==========

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method, ITestContext context) {
        LogUtils.info(getClass(), "🚀 Test: {}.{}",
                method.getDeclaringClass().getSimpleName(), method.getName());

        DriverManager.startDriver();

        ACTIONS.set(new ElementActions(DriverManager.getDriver()));
        SCREENSHOT.set(new ScreenshotHelper(DriverManager.getDriver()));
        WAIT.set(WaitStrategyFactory.getStrategy(DriverManager.getDriver()));

        Allure.epic(context.getSuite().getName());
        Allure.feature(method.getDeclaringClass().getSimpleName());
        Allure.story(method.getName());

        testSetUp(method);
    }

    // ========== AfterMethod ==========

    @AfterMethod(alwaysRun = true)
    public void tearDown(Method method) {
        try {
            testTearDown();
        } finally {
            DriverManager.quitDriver();
            ACTIONS.remove();
            SCREENSHOT.remove();
            WAIT.remove();
        }
    }

    // ========== AfterSuite ==========

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        // 清理测试数据生命周期
        DataLifecycleManager.getInstance().cleanupAll();
        LogUtils.info(getClass(), "Suite finished");
    }

    // ========== 子类钩子 ==========

    protected void testSetUp(Method method) {}
    protected void testTearDown() {}
}
