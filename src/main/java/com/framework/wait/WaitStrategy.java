package com.framework.wait;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * ===========================================
 * ⏱️ 等待策略接口 (Strategy Pattern)
 * ===========================================
 *
 * 不同的等待策略实现这个接口。
 * 默认使用显式等待，也可以切换为轮询等待、重试等待等。
 *
 * 使用方式：
 *   WaitStrategy strategy = new ExplicitWaitStrategy(driver, 30);
 *   strategy.waitForVisible(By.id("login-btn"));
 */
public interface WaitStrategy {

    /** 等元素可见 */
    WebElement waitForVisible(By locator);

    /** 等元素可点击 */
    WebElement waitForClickable(By locator);

    /** 等元素出现在 DOM 中（不一定可见） */
    WebElement waitForPresence(By locator);

    /** 等元素消失 */
    boolean waitForInvisible(By locator);

    /** 等元素文本为指定值 */
    boolean waitForText(By locator, String text);

    /** 等页面加载完毕 */
    void waitForPageLoad(WebDriver driver);
}
