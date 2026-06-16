package com.framework.browser;

import com.framework.utils.LogUtils;
import org.openqa.selenium.WebDriver;

import java.util.Objects;

/**
 * ===========================================
 * 🚗 驱动管理器 (DriverManager)
 * ===========================================
 *
 * 管理 WebDriver 的生命周期。
 * 核心设计：每个线程有自己的 WebDriver（ThreadLocal）。
 * 这样多个测试并行跑时不会互相干扰。
 *
 * 使用方式（通常不需要自己调，BaseTest 已经帮你做好了）：
 *
 *   // 启动浏览器
 *   DriverManager.startDriver();
 *
 *   // 获取 driver
 *   WebDriver driver = DriverManager.getDriver();
 *
 *   // 关闭浏览器
 *   DriverManager.quitDriver();
 */
public class DriverManager {

    /**
     * ThreadLocal：每个线程存一份自己的 WebDriver。
     * 线程 A 的 driver 不会影响线程 B 的 driver。
     */
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
        // 工具类，不允许实例化
    }

    /**
     * 获取当前线程的 WebDriver。
     * 如果还没启动，会抛异常提醒你。
     *
     * @return 当前线程的 WebDriver
     * @throws NullPointerException 如果 driver 还没初始化
     */
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        Objects.requireNonNull(driver,
                "当前线程的 WebDriver 还没初始化！" +
                "你是不是忘了调 DriverManager.startDriver()？");
        return driver;
    }

    /**
     * 启动一个新浏览器，绑定到当前线程。
     * 用 BrowserFactory 创建 driver，具体创建什么浏览器看 config.yaml。
     *
     * @return 新创建的 WebDriver
     */
    public static WebDriver startDriver() {
        WebDriver driver = BrowserFactory.createDriver();  // 根据配置创建浏览器
        DRIVER.set(driver);                                 // 绑定到当前线程
        return driver;
    }

    /**
     * 关闭当前线程的浏览器，清理资源。
     * 安全关闭——即使 driver 已经挂了也不会抛异常。
     */
    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();  // 关闭浏览器
                LogUtils.info(DriverManager.class, "🛑 浏览器已关闭");
            } catch (Exception e) {
                LogUtils.warn(DriverManager.class, "关闭浏览器时出错: {}", e.getMessage());
            } finally {
                DRIVER.remove();  // 清理 ThreadLocal，防止内存泄漏
            }
        }
    }

    /**
     * 检查当前线程是否有活动的浏览器。
     *
     * @return true = 浏览器还开着
     */
    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }
}
