package com.framework.core.hook;

import com.framework.ai.client.AIClient;
import com.framework.ai.analysis.FlakyTestAnalyzer;
import com.framework.utils.LogUtils;
import io.qameta.allure.Allure;
import org.testng.ISuite;

import java.util.List;

/**
 * Flaky Test 检测 Hook。
 * 单一职责：Suite 结束时自动检测不稳定用例。
 */
public class FlakyDetectionHook implements TestEventHook {

    @Override
    public void onSuiteFinish(ISuite suite) {
        if (!AIClient.isReady()) return;

        try {
            List<FlakyTestAnalyzer.FlakyReport> flaky = FlakyTestAnalyzer.analyzeWithAI(7);
            if (flaky.isEmpty()) return;

            StringBuilder report = new StringBuilder();
            report.append("\n🔍 Flaky Test 检测报告 (最近7天)\n");
            report.append("═══════════════════════════════════\n");
            for (FlakyTestAnalyzer.FlakyReport f : flaky) {
                report.append(f.toString()).append("\n");
            }
            report.append("═══════════════════════════════════\n");
            LogUtils.info(getClass(), report.toString());
            Allure.addAttachment("Flaky Test 分析", "text/plain", report.toString());
        } catch (Exception e) {
            LogUtils.warn(getClass(), "Flaky 检测异常: {}", e.getMessage());
        }
    }
}
