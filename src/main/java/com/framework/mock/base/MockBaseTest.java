package com.framework.mock.base;

import com.framework.mock.driver.MockWebDriver;
import com.framework.utils.LogUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

/**
 * ===========================================
 * 🎭 Mock 测试基类 — 零环境依赖学习模式
 * ===========================================
 *
 * 继承此类的所有测试在 MockWebDriver 下运行，
 * 无需安装任何浏览器或驱动。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * public class MockLoginUITest extends MockBaseTest {
 *     @Test
 *     public void testLogin() {
 *         AnnotatedLoginPage page = new AnnotatedLoginPage(mockDriver);
 *         page.navigateTo().login("admin", "pass");
 *         assert mockDriver.hasPerformed("admin");
 *     }
 * }
 * }</pre>
 *
 * <h3>切换到真实浏览器</h3>
 * 只需要改一行：extends MockBaseTest → extends BaseTest
 * Page Object 代码完全不用动。
 *
 * @author Lee
 * @since 3.2.0
 */
public abstract class MockBaseTest {

    protected MockWebDriver mockDriver;

    @BeforeMethod(alwaysRun = true)
    public void setUpMock(Method method) {
        mockDriver = new MockWebDriver();
        LogUtils.info(getClass(), "🎭 [Mock] 开始: {}.{}",
                method.getDeclaringClass().getSimpleName(), method.getName());
        onSetUp(method);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownMock(Method method) {
        try {
            onTearDown();
        } finally {
            mockDriver.printTimeline();
            mockDriver.quit();
        }
    }

    protected void onSetUp(Method method) {}
    protected void onTearDown() {}
}
