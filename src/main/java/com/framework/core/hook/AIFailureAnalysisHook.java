package com.framework.core.hook;

import com.framework.ai.client.AIClient;
import com.framework.ai.analysis.FailureAnalyzer;
import com.framework.utils.LogUtils;
import io.qameta.allure.Allure;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import com.framework.core.ConfigManager;
import com.framework.browser.DriverManager;

/**
 * AI 失败诊断 Hook。
 * 单一职责：测试失败时用 AI 分析根因。
 */
public class AIFailureAnalysisHook implements TestEventHook {

    @Override
    public void onTestFailure(ITestResult result) {
        if (!AIClient.isReady()) return;
        if (!ConfigManager.get().aiConfig().getFailureAnalysis().isEnabled()) return;

        try {
            WebDriver driver = DriverManager.hasDriver() ? DriverManager.getDriver() : null;
            Throwable t = result.getThrowable();
            String diagnosis = FailureAnalyzer.analyze(driver, t, result.getName());
            if (diagnosis != null) {
                FailureAnalyzer.FailureResult parsed = FailureAnalyzer.parseResult(diagnosis);
                if (parsed != null) {
                    LogUtils.info(getClass(), "🤖 AI 诊断: {}", parsed);
                    Allure.addAttachment("AI 失败诊断", "text/plain", parsed.toString());
                }
            }
        } catch (Exception e) {
            LogUtils.warn(getClass(), "AI 诊断调用失败: {}", e.getMessage());
        }
    }
}
