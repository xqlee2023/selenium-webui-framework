package com.framework.wait;

import com.framework.core.ConfigManager;
import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ===========================================
 * 🏭 等待策略工厂 (Factory Pattern)
 * ===========================================
 *
 * 统一获取等待策略实例。
 * 从 ConfigManager 读取等待策略类型，支持运行时动态注册自定义策略。
 * 默认使用显式等待。
 *
 * 使用方式：
 *   WaitStrategy strategy = WaitStrategyFactory.getStrategy(driver);
 *   strategy.waitForVisible(By.id("login-btn"));
 */
public class WaitStrategyFactory {

    private static final Map<String, StrategySupplier> STRATEGIES = new ConcurrentHashMap<>();

    static {
        // 注册内置策略
        register("explicit", ExplicitWaitStrategy::new);
    }

    private WaitStrategyFactory() {}

    /**
     * 注册自定义等待策略。
     *
     * @param name     策略名称（从 config.yaml 中 strategy.waitStrategy 字段引用）
     * @param supplier 策略工厂函数
     */
    public static void register(String name, StrategySupplier supplier) {
        STRATEGIES.put(name, supplier);
    }

    /**
     * 根据配置获取等待策略。
     * 策略类型由 config.yaml 中 strategy.waitStrategy 控制，默认 "explicit"。
     */
    public static WaitStrategy getStrategy(WebDriver driver) {
        String type = ConfigManager.get().getExecution().waitStrategy;
        // 兼容旧配置
        if (type == null || type.isBlank()) type = "explicit";
        StrategySupplier supplier = STRATEGIES.get(type);
        if (supplier == null) {
            supplier = STRATEGIES.get("explicit");
        }
        return supplier.create(driver);
    }

    /**
     * 获取指定超时时间的等待策略。
     * 注意：此方法始终返回 ExplicitWaitStrategy（自定义超时），
     * 不跟随 STRATEGIES 注册表。如果需要自定义策略的自定义超时，
     * 请在注册时通过 StrategySupplier 同时提供 timeout 支持。
     */
    public static WaitStrategy getStrategy(WebDriver driver, int timeoutSeconds) {
        return new ExplicitWaitStrategy(driver, timeoutSeconds);
    }

    @FunctionalInterface
    public interface StrategySupplier {
        WaitStrategy create(WebDriver driver);
    }
}
