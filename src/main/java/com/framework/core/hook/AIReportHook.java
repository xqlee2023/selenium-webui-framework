package com.framework.core.hook;

import com.framework.ai.client.AIClient;
import com.framework.ai.generator.AIReportGenerator;
import com.framework.utils.LogUtils;
import io.qameta.allure.Allure;
import org.testng.ISuite;
import org.testng.ITestResult;

import java.time.Instant;

/**
 * AI 报告生成 Hook。
 * 单一职责：收集执行数据，Suite 结束时生成自然语言报告。
 */
public class AIReportHook implements TestEventHook {

    private final AIReportGenerator reportGenerator = AIReportGenerator.create();
    private Instant suiteStartInstant;

    @Override
    public void onSuiteStart(ISuite suite) {
        suiteStartInstant = Instant.now();
        reportGenerator.suite(suite.getName()).startTime(suiteStartInstant);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        reportGenerator.record(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        reportGenerator.record(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        reportGenerator.record(result);
    }

    @Override
    public void onSuiteFinish(ISuite suite) {
        if (!AIClient.isReady()) return;

        reportGenerator.endTime(Instant.now());
        try {
            String aiReport = reportGenerator.generate();
            LogUtils.info(getClass(), "\n{}", aiReport);
            Allure.addAttachment("AI 测试报告", "text/plain", aiReport);
        } catch (Exception e) {
            LogUtils.warn(getClass(), "AI 报告生成失败: {}", e.getMessage());
        }
    }
}
