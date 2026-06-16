package com.framework.element.healing;

import com.framework.utils.LogUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

/**
 * ===========================================
 * 🔗 组合自愈策略 — 责任链模式
 * ===========================================
 *
 * 按注册顺序串联多个自愈策略。
 * 每个策略依次尝试，命中即返回，全部失败则抛异常。
 *
 * 典型用法：
 *   new CompositeHealingStrategy(
 *       new HealeniumHealingStrategy(),  // 快：历史数据自愈
 *       new AIHealingStrategy()          // 准：LLM 推理兜底
 *   );
 *
 * 策略执行流程：
 *   Healenium (毫秒级, 离线) → 失败 ↓
 *   AI (秒级, 需API) → 失败 ↓
 *   → 抛出原始 NoSuchElementException
 */
public class CompositeHealingStrategy implements ElementHealingStrategy {

    private final List<ElementHealingStrategy> strategies;

    public CompositeHealingStrategy(ElementHealingStrategy... strategies) {
        this.strategies = Arrays.asList(strategies);
    }

    @Override
    public WebElement heal(By failedLocator, String pageArea, WebDriver driver) {
        NoSuchElementException lastException = null;

        for (int i = 0; i < strategies.size(); i++) {
            ElementHealingStrategy strategy = strategies.get(i);
            String strategyName = strategy.getClass().getSimpleName();

            try {
                LogUtils.debug(getClass(), "🔧 自愈尝试 {}/{}: {}",
                        i + 1, strategies.size(), strategyName);

                WebElement healed = strategy.heal(failedLocator, pageArea, driver);
                if (healed != null) {
                    return healed;
                }
            } catch (NoSuchElementException e) {
                lastException = e;
                LogUtils.debug(getClass(), "  ↳ {} 自愈失败，尝试下一个", strategyName);
            } catch (Exception e) {
                LogUtils.warn(getClass(), "  ↳ {} 自愈异常: {}", strategyName, e.getMessage());
                if (e instanceof NoSuchElementException nse) {
                    lastException = nse;
                }
            }
        }

        // 所有策略均失败
        String errorMsg = "所有 " + strategies.size() + " 个自愈策略均失败: " +
                describeLocator(failedLocator);
        LogUtils.error(getClass(), errorMsg);
        throw lastException != null
                ? lastException
                : new NoSuchElementException(errorMsg);
    }

    /** 获取已注册的策略数量 */
    public int strategyCount() {
        return strategies.size();
    }

    private String describeLocator(By locator) {
        return locator.toString().replace("By.", "").replace(": ", "=");
    }
}
