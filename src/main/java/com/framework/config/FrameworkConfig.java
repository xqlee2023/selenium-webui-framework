package com.framework.config;

import com.framework.ai.client.AIConfig;
import com.framework.enums.BrowserType;
import com.framework.enums.EnvironmentType;
import com.framework.enums.ExecutionMode;
import com.framework.config.accessibility.AccessibilityConfig;
import com.framework.config.browser.BrowserOptionsConfig;
import com.framework.config.browser.TimeoutConfig;
import com.framework.config.execution.ExecutionConfig;
import com.framework.config.reporting.ReportingConfig;

/**
 * 框架总配置 POJO（对应 config.yaml 结构）。
 * 由 ConfigManager 加载和填充。
 */
public class FrameworkConfig {

    private BrowserType browser = BrowserType.CHROME;
    private EnvironmentType environment = EnvironmentType.DEV;
    private ExecutionMode executionMode = ExecutionMode.LOCAL;
    private String baseUrl = "https://example.com";
    private String hubUrl = "http://localhost:4444/wd/hub";
    private TimeoutConfig timeouts = new TimeoutConfig();
    private BrowserOptionsConfig browserOptions = new BrowserOptionsConfig();
    private ReportingConfig reporting = new ReportingConfig();
    private ExecutionConfig execution = new ExecutionConfig();
    private AIConfig ai = new AIConfig();
    private AccessibilityConfig accessibility = new AccessibilityConfig();

    // Getters
    public BrowserType getBrowser() { return browser; }
    public EnvironmentType getEnvironment() { return environment; }
    public ExecutionMode getExecutionMode() { return executionMode; }
    public String getBaseUrl() { return baseUrl; }
    public String getHubUrl() { return hubUrl; }
    public TimeoutConfig getTimeouts() { return timeouts; }
    public BrowserOptionsConfig getBrowserOptions() { return browserOptions; }
    public ReportingConfig getReporting() { return reporting; }
    public ExecutionConfig getExecution() { return execution; }

    // Setters
    public void setBrowser(BrowserType browser) { this.browser = browser; }
    public void setEnvironment(EnvironmentType environment) { this.environment = environment; }
    public void setExecutionMode(ExecutionMode executionMode) { this.executionMode = executionMode; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public void setHubUrl(String hubUrl) { this.hubUrl = hubUrl; }
    public void setTimeouts(TimeoutConfig timeouts) { this.timeouts = timeouts; }
    public void setBrowserOptions(BrowserOptionsConfig browserOptions) { this.browserOptions = browserOptions; }
    public void setReporting(ReportingConfig reporting) { this.reporting = reporting; }
    public void setExecution(ExecutionConfig execution) { this.execution = execution; }
    public AIConfig getAi() { return ai; }
    public void setAi(AIConfig ai) { this.ai = ai; }
    public AccessibilityConfig getAccessibility() { return accessibility; }
    public void setAccessibility(AccessibilityConfig accessibility) { this.accessibility = accessibility; }
}
