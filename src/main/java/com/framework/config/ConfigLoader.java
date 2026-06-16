package com.framework.config;

import com.framework.utils.LogUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

/**
 * ===========================================
 * 📂 配置加载器 (ConfigLoader) — 单一职责
 * ===========================================
 *
 * 只负责 YAML 读取 + 环境覆盖 + 系统属性覆盖。
 * 从 ConfigManager 中拆分出来，遵循 SRP。
 *
 * 配置优先级：config.yaml → config-{env}.yaml → 系统属性
 */
public class ConfigLoader {

    private static final String DEFAULT_CONFIG = "/config/config.yaml";
    private static final String DEFAULT_ENV = "dev";

    /**
     * 完整加载配置：基础 YAML → 环境覆盖 → 系统属性覆盖。
     */
    public FrameworkConfig load() {
        String activeEnv = resolveEnvironment();
        FrameworkConfig config = loadBaseConfig();
        applyEnvironmentOverrides(config, activeEnv);
        applySystemPropertyOverrides(config);
        LogUtils.info(getClass(), "✅ 配置加载完成 | 环境={}", activeEnv);
        return config;
    }

    // ========== 内部步骤 ==========

    private String resolveEnvironment() {
        return System.getProperty("env",
                System.getenv().getOrDefault("TEST_ENV", DEFAULT_ENV));
    }

    private FrameworkConfig loadBaseConfig() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            InputStream stream = getClass().getResourceAsStream(DEFAULT_CONFIG);
            if (stream != null) {
                return mapper.readValue(stream, FrameworkConfig.class);
            }
            File file = new File("src/main/resources/config/config.yaml");
            if (file.exists()) {
                return mapper.readValue(file, FrameworkConfig.class);
            }
        } catch (Exception e) {
            LogUtils.warn(getClass(), "基础配置加载失败: {}", e.getMessage());
        }
        return new FrameworkConfig();
    }

    private void applyEnvironmentOverrides(FrameworkConfig config, String env) {
        String envFile = "/config/config-" + env + ".yaml";
        try {
            InputStream stream = getClass().getResourceAsStream(envFile);
            if (stream != null) {
                FrameworkConfig overrides = new ObjectMapper(new YAMLFactory())
                        .readValue(stream, FrameworkConfig.class);
                merge(config, overrides);
            }
        } catch (Exception ignored) {
            // 环境配置文件不存在是正常的
        }
    }

    private void applySystemPropertyOverrides(FrameworkConfig config) {
        Optional.ofNullable(System.getProperty("browser"))
                .ifPresent(v -> config.setBrowser(
                        com.framework.enums.BrowserType.fromString(v)));
        Optional.ofNullable(System.getProperty("baseUrl"))
                .ifPresent(config::setBaseUrl);
        Optional.ofNullable(System.getProperty("headless"))
                .ifPresent(v -> config.getBrowserOptions().headless =
                        Boolean.parseBoolean(v));
    }

    /**
     * 将 overrides 中的非 null 字段合并到 base。
     * 完整覆盖所有配置层级：timeouts、browserOptions、reporting、execution、AI、accessibility。
     */
    private void merge(FrameworkConfig base, FrameworkConfig overrides) {
        if (overrides.getBaseUrl() != null) base.setBaseUrl(overrides.getBaseUrl());
        if (overrides.getHubUrl() != null) base.setHubUrl(overrides.getHubUrl());
        if (overrides.getBrowser() != null) base.setBrowser(overrides.getBrowser());
        if (overrides.getEnvironment() != null) base.setEnvironment(overrides.getEnvironment());
        if (overrides.getExecutionMode() != null) base.setExecutionMode(overrides.getExecutionMode());
        // Timeouts
        if (overrides.getTimeouts() != null) {
            if (overrides.getTimeouts().explicitWait > 0) base.getTimeouts().explicitWait = overrides.getTimeouts().explicitWait;
            if (overrides.getTimeouts().implicitWait > 0) base.getTimeouts().implicitWait = overrides.getTimeouts().implicitWait;
            if (overrides.getTimeouts().pageLoadWait > 0) base.getTimeouts().pageLoadWait = overrides.getTimeouts().pageLoadWait;
            if (overrides.getTimeouts().pollingInterval > 0) base.getTimeouts().pollingInterval = overrides.getTimeouts().pollingInterval;
        }
        // Browser options
        if (overrides.getBrowserOptions() != null) {
            var src = overrides.getBrowserOptions();
            var dst = base.getBrowserOptions();
            if (src.headless != null) dst.headless = src.headless;
            if (src.maximize != null) dst.maximize = src.maximize;
            if (src.arguments != null && !src.arguments.isEmpty()) dst.arguments = src.arguments;
            if (src.preferences != null && !src.preferences.isEmpty()) dst.preferences = src.preferences;
        }
        // Reporting
        if (overrides.getReporting() != null) {
            base.getReporting().screenshotOnFailure = overrides.getReporting().screenshotOnFailure;
            base.getReporting().retryOnFailure = overrides.getReporting().retryOnFailure;
            if (overrides.getReporting().retryCount > 0) base.getReporting().retryCount = overrides.getReporting().retryCount;
            if (overrides.getReporting().allureResultsDir != null) base.getReporting().allureResultsDir = overrides.getReporting().allureResultsDir;
            if (overrides.getReporting().screenshotDir != null) base.getReporting().screenshotDir = overrides.getReporting().screenshotDir;
        }
        // Execution
        if (overrides.getExecution() != null) {
            if (overrides.getExecution().parallelCount > 0) base.getExecution().parallelCount = overrides.getExecution().parallelCount;
            if (overrides.getExecution().waitStrategy != null) base.getExecution().waitStrategy = overrides.getExecution().waitStrategy;
        }
        // AI config (deep merge)
        if (overrides.getAi() != null) {
            var src = overrides.getAi();
            var dst = base.getAi();
            dst.setEnabled(src.isEnabled());
            if (src.getProvider() != null) dst.setProvider(src.getProvider());
            if (src.getApiKey() != null && !src.getApiKey().isBlank()) dst.setApiKey(src.getApiKey());
            if (src.getModel() != null) dst.setModel(src.getModel());
            if (src.getEndpoint() != null) dst.setEndpoint(src.getEndpoint());
            if (src.getMaxTokens() > 0) dst.setMaxTokens(src.getMaxTokens());
            if (src.getTemperature() > 0) dst.setTemperature(src.getTemperature());
            if (src.getMaxRetries() > 0) dst.setMaxRetries(src.getMaxRetries());
            // AI sub-configs
            if (src.getFailureAnalysis() != null) {
                dst.getFailureAnalysis().setEnabled(src.getFailureAnalysis().isEnabled());
            }
            if (src.getSelfHealing() != null) {
                dst.getSelfHealing().setEnabled(src.getSelfHealing().isEnabled());
            }
            if (src.getReport() != null) {
                dst.getReport().setEnabled(src.getReport().isEnabled());
            }
        }
        // Accessibility
        if (overrides.getAccessibility() != null) {
            base.getAccessibility().enabled = overrides.getAccessibility().enabled;
            base.getAccessibility().onSuccessOnly = overrides.getAccessibility().onSuccessOnly;
            if (overrides.getAccessibility().minImpactLevel != null) {
                base.getAccessibility().minImpactLevel = overrides.getAccessibility().minImpactLevel;
            }
        }
    }
}
