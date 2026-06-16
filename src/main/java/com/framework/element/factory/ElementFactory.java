package com.framework.element.factory;

import com.framework.browser.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * ===========================================
 * 🏭 元素工厂 (Factory Pattern)
 * ===========================================
 *
 * 统一创建 WebElement 的入口。
 * 封装了查找逻辑，支持 懒加载 和 缓存策略。
 * 后续可扩展加入 AI 智能定位、Shadow DOM 穿透等能力。
 *
 * 使用方式：
 *   WebElement btn = ElementFactory.find(driver, By.id("login"));
 *   List<WebElement> items = ElementFactory.findAll(driver, By.cssSelector(".item"));
 */
public class ElementFactory {

    private ElementFactory() {}

    /**
     * 查找单个元素。如果元素不存在会立即抛异常。
     */
    public static WebElement find(SearchContext context, By locator) {
        return context.findElement(locator);
    }

    /**
     * 查找所有匹配元素。不会抛异常，找不到返回空列表。
     */
    public static List<WebElement> findAll(SearchContext context, By locator) {
        return context.findElements(locator);
    }

    /**
     * 获取元素文本，不存在时返回空字符串（不抛异常）。
     */
    public static String safeText(SearchContext context, By locator) {
        try {
            return context.findElement(locator).getText();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 判断元素是否存在（不管是否可见）。
     */
    public static boolean exists(SearchContext context, By locator) {
        return !context.findElements(locator).isEmpty();
    }

    /**
     * 带重试的查找
     */
    public static WebElement findWithRetry(By locator, int retries, long intervalMs) {
        for (int i = 0; i < retries; i++) {
            try {
                return DriverManager.getDriver().findElement(locator);
            } catch (Exception e) {
                if (i == retries - 1) throw e;
                try { Thread.sleep(intervalMs); } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new org.openqa.selenium.NoSuchElementException("Element not found after " + retries + " retries: " + locator);
    }
}
