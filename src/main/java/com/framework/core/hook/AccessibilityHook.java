package com.framework.core.hook;

import com.framework.ai.accessibility.AccessibilityScanner;
import com.framework.utils.LogUtils;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import org.testng.ISuite;
import org.testng.ITestResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * ===========================================
 * ♿ 无障碍检测 Hook
 * ===========================================
 *
 * 每个测试方法执行后（成功或失败），自动扫描当前页面的无障碍状况。
 * Suite 结束时汇总所有违规，写入 Allure 报告附件。
 *
 * <h3>触发时机</h3>
 * <ul>
 *   <li>测试成功 → 扫描 → 有违规则附加到 Allure</li>
 *   <li>测试失败 → 扫描 → 有违规则附加到 Allure</li>
 *   <li>Suite 结束 → 汇总报告 → 打印日志</li>
 * </ul>
 *
 * <h3>严重度分级（WCAG）</h3>
 * <ul>
 *   <li>🔴 Critical  — 用户完全无法使用</li>
 *   <li>🟠 Serious   — 严重影响使用</li>
 *   <li>🟡 Moderate  — 使用不便</li>
 *   <li>🔵 Minor     — 小优化建议</li>
 * </ul>
 *
 * @author Lee
 * @since 3.2.0
 */
public class AccessibilityHook implements TestEventHook {

    /** 累计所有违规（跨测试合并） */
    private final List<AccessibilityScanner.Violation> allViolations = new ArrayList<>();

    /** 总通过规则数 */
    private int totalPasses = 0;
    /** 总扫描次数 */
    private int scanCount = 0;

    @Override
    public void onTestSuccess(ITestResult result) {
        scanAndAttach(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        scanAndAttach(result);
    }

    @Override
    public void onSuiteFinish(ISuite suite) {
        if (scanCount == 0) {
            LogUtils.info(getClass(), "♿ 无障碍检测: 无页面扫描记录");
            return;
        }

        LogUtils.info(getClass(), "═══════════════════════════════════════");
        LogUtils.info(getClass(), "♿ 无障碍检测汇总");
        LogUtils.info(getClass(), "   扫描 {} 个页面, 通过规则 {} 条, 违规 {} 项",
                scanCount, totalPasses, allViolations.size());

        if (!allViolations.isEmpty()) {
            long critical = allViolations.stream()
                    .filter(v -> "critical".equals(v.impact())).count();
            long serious = allViolations.stream()
                    .filter(v -> "serious".equals(v.impact())).count();
            long moderate = allViolations.stream()
                    .filter(v -> "moderate".equals(v.impact())).count();
            long minor = allViolations.stream()
                    .filter(v -> "minor".equals(v.impact())).count();

            LogUtils.info(getClass(), "   🔴 Critical: {}  🟠 Serious: {}  🟡 Moderate: {}  🔵 Minor: {}",
                    critical, serious, moderate, minor);

            // 写 Markdown 报告到 Allure
            String report = buildSummaryReport(critical, serious, moderate, minor);
            Allure.addAttachment("♿ 无障碍检测报告", "text/markdown", report, ".md");
        } else {
            LogUtils.info(getClass(), "   ✅ 所有页面无障碍检测通过！");
        }
        LogUtils.info(getClass(), "═══════════════════════════════════════");
    }

    private void scanAndAttach(ITestResult result) {
        try {
            var driver = com.framework.browser.DriverManager.getDriver();
            if (driver == null) return;

            AccessibilityScanner.Result scanResult = AccessibilityScanner.scan(driver);
            if (scanResult == null) return;

            scanCount++;
            totalPasses += scanResult.passes();

            if (scanResult.hasViolations()) {
                allViolations.addAll(scanResult.violations());

                // 每个失败页面单独附报告
                String report = scanResult.toMarkdown();
                String testName = result.getMethod().getMethodName();
                Allure.addAttachment(
                        "♿ A11y - " + testName,
                        "text/markdown", report, ".md");
            }
        } catch (Exception e) {
            LogUtils.warn(getClass(), "无障碍扫描异常: {}", e.getMessage());
        }
    }

    private String buildSummaryReport(long critical, long serious, long moderate, long minor) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ♿ 无障碍检测 - Suite 汇总\n\n");
        sb.append("| 指标 | 值 |\n|------|-----|\n");
        sb.append("| 扫描页面数 | ").append(scanCount).append(" |\n");
        sb.append("| 通过规则 | ").append(totalPasses).append(" |\n");
        sb.append("| 总违规数 | ").append(allViolations.size()).append(" |\n\n");

        sb.append("| 严重度 | 数量 |\n|--------|------|\n");
        if (critical > 0) sb.append("| 🔴 Critical | ").append(critical).append(" |\n");
        if (serious > 0)  sb.append("| 🟠 Serious  | ").append(serious).append(" |\n");
        if (moderate > 0) sb.append("| 🟡 Moderate | ").append(moderate).append(" |\n");
        if (minor > 0)    sb.append("| 🔵 Minor    | ").append(minor).append(" |\n");

        sb.append("\n### 违规 Top 5\n\n");
        allViolations.stream()
                .limit(5)
                .forEach(v -> {
                    sb.append("- **").append(v.description()).append("** (`").append(v.id()).append("`)\n");
                    sb.append("  - 影响: ").append(v.impact()).append("\n");
                    if (!v.nodes().isEmpty()) {
                        sb.append("  - 元素: `").append(v.nodes().get(0).html()).append("`\n");
                    }
                    sb.append("  - 修复: [").append(v.help()).append("](").append(v.helpUrl()).append(")\n");
                });

        return sb.toString();
    }
}
