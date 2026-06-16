package com.framework.core;

import com.framework.config.*;
import com.framework.enums.BrowserType;
import com.framework.enums.EnvironmentType;
import com.framework.enums.ExecutionMode;
import com.framework.utils.LogUtils;

import java.util.List;
import java.util.Map;

/**
 * ===========================================
 * ⚙️ 配置管理器 (ConfigManager) — 单例模式
 * ===========================================
 *
 * 整个框架只有一个配置实例。
 * 配置加载委托给 ConfigLoader（遵循 SRP）。
 * AI 初始化已移至 AILifecycleManager。
 *
 * 使用方式：
 *   ConfigManager cfg = ConfigManager.get();
 *   cfg.browser();     // CHROME / FIREFOX
 *   cfg.baseUrl();     // 测试网址
 */
public class ConfigManager {

    private static volatile ConfigManager instance;
    private final FrameworkConfig config;

    private ConfigManager() {
        this.config = new ConfigLoader().load();
        LogUtils.info(ConfigManager.class, "✅ 配置管理器就绪");
    }

    public static ConfigManager init() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    public static ConfigManager get() {
        return instance != null ? instance : init();
    }

    // ========== 配置值获取 ==========

    public BrowserType browser()              { return config.getBrowser(); }
    public EnvironmentType environment()      { return config.getEnvironment(); }
    public ExecutionMode executionMode()      { return config.getExecutionMode(); }
    public String baseUrl()                   { return config.getBaseUrl(); }
    public String hubUrl()                    { return config.getHubUrl(); }
    public int implicitWait()                 { return config.getTimeouts().implicitWait; }
    public int explicitWait()                 { return config.getTimeouts().explicitWait; }
    public int pageLoadWait()                 { return config.getTimeouts().pageLoadWait; }
    public int pollingInterval()              { return config.getTimeouts().pollingInterval; }
    public boolean headless()                 { return config.getBrowserOptions().headless != null ? config.getBrowserOptions().headless : false; }
    public boolean maximize()                 { return config.getBrowserOptions().maximize != null ? config.getBrowserOptions().maximize : true; }
    public List<String> browserArguments()    { return config.getBrowserOptions().arguments; }
    public Map<String, Object> browserPreferences() { return config.getBrowserOptions().preferences; }
    public String allureResultsDir()          { return config.getReporting().allureResultsDir; }
    public String screenshotDir()             { return config.getReporting().screenshotDir; }
    public boolean screenshotOnFailure()      { return config.getReporting().screenshotOnFailure; }
    public boolean retryOnFailure()           { return config.getReporting().retryOnFailure; }
    public int retryCount()                   { return config.getReporting().retryCount; }
    public com.framework.config.execution.ExecutionConfig getExecution() { return config.getExecution(); }

    public int parallelCount()                { return config.getExecution().parallelCount; }
    public boolean aiEnabled()                { return config.getAi().isEnabled(); }
    public boolean accessibilityEnabled()     { return config.getAccessibility() != null && config.getAccessibility().enabled; }
    public com.framework.ai.client.AIConfig aiConfig() { return config.getAi(); }
}
