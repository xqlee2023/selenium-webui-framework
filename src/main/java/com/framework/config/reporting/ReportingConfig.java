package com.framework.config.reporting;

/** 报告配置。 */
public class ReportingConfig {
    public boolean screenshotOnFailure = true;
    public boolean retryOnFailure = true;
    public int retryCount = 2;
    public String allureResultsDir = "allure-results";
    public String screenshotDir = "screenshots";
}
