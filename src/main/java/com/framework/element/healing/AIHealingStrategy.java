package com.framework.element.healing;

import com.framework.ai.healing.SelfHealingLocator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * AI 驱动的自愈策略。
 * 委托给 SelfHealingLocator 完成 AI 推导和重试。
 */
public class AIHealingStrategy implements ElementHealingStrategy {

    @Override
    public WebElement heal(By failedLocator, String pageArea, WebDriver driver) {
        return new SelfHealingLocator(driver).heal(failedLocator, pageArea);
    }
}
