package com.framework.element.healing;

import com.framework.utils.LogUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Method;

/**
 * ===========================================
 * 🩺 Healenium 定位器自愈策略
 * ===========================================
 *
 * 基于 Healenium 的历史定位器成功记录做自愈。
 * 当原始定位器失败时，Healenium 查询该元素历史上成功过的备选定位器并逐个尝试。
 *
 * 特点：
 *   - 毫秒级响应（本地查库）
 *   - 完全离线，无 API 成本
 *   - 对首次遇到的元素无效（需要积累历史数据）
 *
 * 优雅降级：
 *   - 如果 Healenium 库不在 classpath → 直接抛出异常交给下一层策略
 *   - 使用反射调用避免编译时依赖 healenium-web
 *
 * 与 CompositeHealingStrategy 配合：
 *   new CompositeHealingStrategy(
 *       new HealeniumHealingStrategy(),  // 第1优先：快速本地
 *       new AIHealingStrategy()          // 第2兜底：LLM 推理
 *   );
 *
 * 启用方式：
 *   1. pom.xml 添加 healenium-web 依赖
 *   2. 无需配置，classpath 检测自动启用
 */
public class HealeniumHealingStrategy implements ElementHealingStrategy {

    private static final boolean HEALENIUM_AVAILABLE;
    private static final String DRIVER_CLASS = "com.epam.healenium.SelfHealingDriver";

    static {
        boolean available = false;
        try {
            Class.forName(DRIVER_CLASS);
            available = true;
            LogUtils.info(HealeniumHealingStrategy.class, "✅ Healenium SelfHealingDriver 已检测到，自愈已启用");
        } catch (ClassNotFoundException ignored) {
            LogUtils.warn(HealeniumHealingStrategy.class,
                    "⚠️ Healenium 未在 classpath 中。"
                    + "如需启用基于历史数据的快速自愈，请在 pom.xml 中取消 healenium-web 依赖的注释。");
        }
        HEALENIUM_AVAILABLE = available;
    }

    @Override
    public WebElement heal(By failedLocator, String pageArea, WebDriver driver) {
        if (!HEALENIUM_AVAILABLE) {
            throw new NoSuchElementException(
                    "Healenium 不可用（未添加 healenium-web 依赖），跳过自愈: "
                    + describeLocator(failedLocator));
        }

        try {
            long start = System.currentTimeMillis();

            // 反射调用：SelfHealingDriver.create(driver).findElement(locator)
            Class<?> driverClass = Class.forName(DRIVER_CLASS);
            Method createMethod = driverClass.getMethod("create", WebDriver.class);
            Object healingDriver = createMethod.invoke(null, driver);
            WebElement element = (WebElement) healingDriver.getClass()
                    .getMethod("findElement", By.class)
                    .invoke(healingDriver, failedLocator);

            long elapsed = System.currentTimeMillis() - start;
            LogUtils.info(getClass(), "✅ Healenium 自愈成功! {} ({}ms)",
                    describeLocator(failedLocator), elapsed);

            return element;
        } catch (NoSuchElementException e) {
            throw e;
        } catch (Exception e) {
            LogUtils.debug(getClass(), "Healenium 自愈失败: {}", e.getMessage());
            throw new NoSuchElementException(
                    "Healenium 自愈失败: " + describeLocator(failedLocator), e);
        }
    }

    public static boolean isAvailable() {
        return HEALENIUM_AVAILABLE;
    }

    private String describeLocator(By locator) {
        return locator.toString().replace("By.", "").replace(": ", "=");
    }
}
