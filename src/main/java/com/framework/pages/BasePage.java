package com.framework.pages;

import com.framework.core.ConfigManager;
import com.framework.browser.DriverManager;
import com.framework.element.actions.ElementActions;
import com.framework.wait.WaitStrategy;
import com.framework.wait.WaitStrategyFactory;
import com.framework.utils.LogUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;

/**
 * ===========================================
 * 🏠 页面对象基类 (BasePage) — 模板方法模式
 * ===========================================
 *
 * 所有页面对象继承此类。
 * 依赖通过参数传递（DI），不自行创建。
 *
 * 子类用法：
 *   public class LoginPage extends BasePage {
 *       public LoginPage(WebDriver driver) {
 *           super(driver);
 *       }
 *   }
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final ElementActions actions;
    protected final WaitStrategy wait;
    protected final ConfigManager config;

    /**
     * 构造时注入 driver，避免隐式依赖 DriverManager。
     * 支持从不同 driver 实例创建页面（比如多窗口场景）。
     */
    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.actions = new ElementActions(driver);
        this.wait = WaitStrategyFactory.getStrategy(driver);
        this.config = ConfigManager.get();
        PageFactory.initElements(
                new AjaxElementLocatorFactory(driver, config.explicitWait()), this);
    }

    /**
     * 默认构造：从当前线程获取 driver。
     */
    protected BasePage() {
        this(DriverManager.getDriver());
    }

    // ========== 子类必须实现 ==========

    /** 页面相对路径，拼在 baseUrl 后面 */
    public abstract String getPageUrl();

    /** 判断是否在当前页面，通常检查关键元素 */
    public abstract boolean isAt();

    // ========== 页面导航 ==========

    @Step("导航到 {this.pageName}")
    public BasePage navigateTo() {
        String url = config.baseUrl() + getPageUrl();
        new com.framework.element.actions.NavigationActions(driver).navigateTo(url);
        wait.waitForPageLoad(driver);
        return this;
    }

    @Step("验证页面加载: {this.pageName}")
    public BasePage verifyPageLoaded() {
        boolean loaded = isAt();
        LogUtils.info(getClass(), "📋 '{}' 加载状态: {}", getPageName(), loaded);
        assert loaded : "页面 " + getPageName() + " 没有正确加载";
        return this;
    }

    // ========== 导航（静态方法） ==========

    /**
     * 直接导航到指定页面并返回页面对象。
     * 用法：LoginPage page = BasePage.goTo(LoginPage.class);
     */
    public static <T extends BasePage> T goTo(Class<T> pageClass) {
        try {
            T page = pageClass.getDeclaredConstructor().newInstance();
            page.navigateTo();
            return page;
        } catch (Exception e) {
            throw new RuntimeException("无法创建页面: " + pageClass.getSimpleName(), e);
        }
    }

    // ========== 便捷方法 ==========

    public String getPageName() {
        return getClass().getSimpleName();
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /** 确保 ElementActions 在页面中也可用（无需从外部传入） */
    protected ElementActions actions() { return actions; }
}
