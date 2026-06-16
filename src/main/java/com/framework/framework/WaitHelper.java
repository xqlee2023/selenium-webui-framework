package com.framework.framework;

import com.framework.core.ConfigManager;
import com.framework.wait.WaitStrategy;
import com.framework.wait.WaitStrategyFactory;
import com.framework.utils.LogUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * ===========================================
 * ⏱️ 等待工具类 (WaitHelper) — 装饰器模式
 * ===========================================
 *
 * 在 WaitStrategy 基础上增加额外等待方法（AJAX、网络空闲等）。
 * 核心元素等待委托给 WaitStrategy，这里只添加增强功能。
 *
 * 使用方式：
 *   WaitHelper wait = new WaitHelper(driver);
 *   wait.waitForPageLoad();    // 等页面加载
 *   wait.waitForAjax();        // 等 AJAX 完成
 */
public class WaitHelper {

    private final WebDriver driver;
    private final WaitStrategy baseWait;
    private static final ConfigManager cfg = ConfigManager.get();

    public WaitHelper(WebDriver driver) {
        this.driver = driver;
        this.baseWait = WaitStrategyFactory.getStrategy(driver);
    }

    // ========== 委托给 WaitStrategy ==========

    public WebElement waitForVisible(By locator)       { return baseWait.waitForVisible(locator); }
    public WebElement waitForClickable(By locator)     { return baseWait.waitForClickable(locator); }
    public WebElement waitForPresence(By locator)      { return baseWait.waitForPresence(locator); }
    public boolean waitForInvisible(By locator)        { return baseWait.waitForInvisible(locator); }
    public boolean waitForText(By locator, String text){ return baseWait.waitForText(locator, text); }
    public void waitForPageLoad()                       { baseWait.waitForPageLoad(driver); }

    // ========== 增强功能 ==========

    /** 等 AJAX 请求完成（jQuery + Angular）。 */
    public void waitForAjax() {
        new WebDriverWait(driver, Duration.ofSeconds(cfg.explicitWait()))
                .pollingEvery(Duration.ofMillis(cfg.pollingInterval()))
                .until(d -> {
                    try {
                        boolean jquery = (boolean) ((JavascriptExecutor) d)
                                .executeScript("return window.jQuery ? jQuery.active === 0 : true");
                        boolean angular = (boolean) ((JavascriptExecutor) d)
                                .executeScript("return window.angular ? " +
                                        "angular.element(document).injector().get('$http').pendingRequests.length === 0 : true");
                        return jquery && angular;
                    } catch (Exception e) {
                        return true;
                    }
                });
    }

    /** 强制等待（毫秒）。仅在特殊情况下使用。 */
    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 智能等待：从多个定位器中等任意一个出现。 */
    public WebElement waitForAny(By... locators) {
        return new WebDriverWait(driver, Duration.ofSeconds(cfg.explicitWait()))
                .until(d -> {
                    for (By loc : locators) {
                        try {
                            WebElement el = d.findElement(loc);
                            if (el.isDisplayed()) return el;
                        } catch (Exception ignored) {}
                    }
                    return null;
                });
    }

    /** 等指定索引的子元素出现。用于动态列表。 */
    public WebElement waitForNthChild(By parentLocator, int index) {
        return new WebDriverWait(driver, Duration.ofSeconds(cfg.explicitWait()))
                .until(d -> {
                    var elements = d.findElements(parentLocator);
                    return elements.size() > index && elements.get(index).isDisplayed()
                            ? elements.get(index) : null;
                });
    }
}
