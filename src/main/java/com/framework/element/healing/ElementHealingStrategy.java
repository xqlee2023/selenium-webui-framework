package com.framework.element.healing;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * ===========================================
 * 🩺 元素自愈策略接口 (OCP 核心)
 * ===========================================
 *
 * 当原始定位器失败时，自愈策略负责推导备选定位器并返回元素。
 * 遵循 OCP：新增自愈策略（AI / 规则 / 历史数据）只需实现此接口。
 */
public interface ElementHealingStrategy {

    /**
     * 尝试自愈定位。
     *
     * @param failedLocator 已失败的原始定位器
     * @param pageArea      元素上下文描述（如 "登录按钮"、"用户名输入框"）
     * @param driver        当前 WebDriver
     * @return 找到的元素，如果自愈也失败则抛出 NoSuchElementException
     */
    WebElement heal(By failedLocator, String pageArea, WebDriver driver);
}
