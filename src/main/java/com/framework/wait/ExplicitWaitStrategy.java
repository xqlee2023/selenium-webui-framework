package com.framework.wait;

import com.framework.core.ConfigManager;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;

/**
 * ===========================================
 * 默认等待策略：显式等待（WebDriverWait）
 * ===========================================
 *
 * 所有等待基于显式等待，超时时间从 config.yaml 读取。
 * 自动忽略 StaleElementReferenceException 和 NoSuchElementException。
 */
public class ExplicitWaitStrategy implements WaitStrategy {

    private final FluentWait<WebDriver> wait;

    public ExplicitWaitStrategy(WebDriver driver) {
        this(driver, ConfigManager.get().explicitWait(), ConfigManager.get().pollingInterval());
    }

    public ExplicitWaitStrategy(WebDriver driver, int timeoutSeconds, int pollingMs) {
        this.wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(pollingMs))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class);
    }

    public ExplicitWaitStrategy(WebDriver driver, int timeoutSeconds) {
        this(driver, timeoutSeconds, ConfigManager.get().pollingInterval());
    }

    @Override
    public WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    @Override
    public WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    @Override
    public WebElement waitForPresence(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    @Override
    public boolean waitForInvisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    @Override
    public boolean waitForText(By locator, String text) {
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    @Override
    public void waitForPageLoad(WebDriver driver) {
        wait.until(d -> {
            try {
                return ((JavascriptExecutor) d)
                        .executeScript("return document.readyState").equals("complete");
            } catch (Exception e) {
                return true;
            }
        });
    }
}
