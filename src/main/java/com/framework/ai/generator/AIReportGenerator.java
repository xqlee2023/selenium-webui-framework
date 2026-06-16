package com.framework.ai.generator;

import com.framework.utils.LogUtils;
import org.testng.ITestContext;
import org.testng.ITestResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.framework.ai.client.AIClient;

/**
 * AI 自然语言报告生成器。
 *
 * 把 TestNG 执行结果发给 AI，生成可读的自然语言摘要。
 * 比 Allure 报告更适合发给非技术人员。
 */
public class AIReportGenerator {

    private static final String SYSTEM_PROMPT = """
            你是一个测试报告分析专家。
            你的任务：把测试执行数据转化为自然语言摘要。
            
            输出 JSON 格式：
            {
              "summary": "一句话总览（如：本次回归通过率 92%，4个用例失败，疑似存在登录模块缺陷）",
              "highlights": ["亮点1", "亮点2"],
              "risks": ["风险1"],
              "flaky_suspects": ["可能不稳定的用例名"],
              "recommendation": "建议（中文，1-2句）"
            }
            
            只输出 JSON，不要有其他内容。""";

    private final List<TestRecord> passed = new ArrayList<>();
    private final List<TestRecord> failed = new ArrayList<>();
    private final List<TestRecord> skipped = new ArrayList<>();
    private Instant startTime;
    private Instant endTime;
    private String suiteName;

    private AIReportGenerator() {}

    public static AIReportGenerator create() {
        return new AIReportGenerator();
    }

    public AIReportGenerator suite(String name) {
        this.suiteName = name;
        return this;
    }

    public AIReportGenerator startTime(Instant time) {
        this.startTime = time;
        return this;
    }

    public AIReportGenerator endTime(Instant time) {
        this.endTime = time;
        return this;
    }

    public AIReportGenerator record(ITestResult result) {
        TestRecord record = new TestRecord();
        record.name = result.getName();
        record.className = result.getTestClass().getName();
        record.durationMs = result.getEndMillis() - result.getStartMillis();
        if (result.getThrowable() != null) {
            record.errorType = result.getThrowable().getClass().getSimpleName();
            record.errorMessage = result.getThrowable().getMessage();
        }
        // 获取分组信息
        String[] groups = result.getMethod().getGroups();
        record.groups = groups.length > 0 ? String.join(",", groups) : "none";

        switch (result.getStatus()) {
            case ITestResult.SUCCESS -> passed.add(record);
            case ITestResult.FAILURE -> failed.add(record);
            case ITestResult.SKIP  -> skipped.add(record);
        }
        return this;
    }

    /**
     * 生成 AI 报告摘要。
     */
    public String generate() {
        if (!AIClient.isReady()) {
            LogUtils.warn(getClass(), "AI 未启用，跳过报告生成");
            return buildFallbackReport();
        }

        long start = System.currentTimeMillis();
        LogUtils.info(getClass(), "🤖 AI 正在生成测试报告...");

        try {
            String userMsg = buildContext();
            String result = AIClient.get().chat(SYSTEM_PROMPT, userMsg);
            long elapsed = System.currentTimeMillis() - start;
            LogUtils.info(getClass(), "🤖 AI 报告生成完成 ({}ms)", elapsed);
            return formatReport(result);
        } catch (Exception e) {
            LogUtils.warn(getClass(), "AI 报告生成失败: {}", e.getMessage());
            return buildFallbackReport();
        }
    }

    private String buildContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("## 测试套件: ").append(suiteName != null ? suiteName : "UI 回归测试").append("\n");
        if (startTime != null && endTime != null) {
            sb.append("执行时间: ").append(Duration.between(startTime, endTime).toMinutes())
              .append(" 分钟\n");
        }

        int total = passed.size() + failed.size() + skipped.size();
        double passRate = total > 0 ? (double) passed.size() / total * 100 : 0;

        sb.append("总计: ").append(total).append(" | ")
          .append("通过: ").append(passed.size()).append(" | ")
          .append("失败: ").append(failed.size()).append(" | ")
          .append("跳过: ").append(skipped.size()).append(" | ")
          .append(String.format("通过率: %.1f%%", passRate)).append("\n\n");

        if (!failed.isEmpty()) {
            sb.append("## 失败用例\n");
            for (TestRecord f : failed) {
                sb.append("- ").append(f.name)
                  .append(" (").append(f.errorType != null ? f.errorType : "Unknown").append(")")
                  .append(": ").append(f.errorMessage != null ? f.errorMessage : "无异常信息")
                  .append(" [" + f.durationMs + "ms]").append("\n");
            }
            sb.append("\n");
        }

        if (!passed.isEmpty() && passed.size() <= 10) {
            sb.append("## 通过用例\n");
            for (TestRecord p : passed) {
                sb.append("- ").append(p.name).append(" [" + p.durationMs + "ms]\n");
            }
            sb.append("\n");
        }

        if (!skipped.isEmpty()) {
            sb.append("## 跳过用例\n");
            for (TestRecord s : skipped) {
                sb.append("- ").append(s.name).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 格式化 AI 返回的 JSON 为可读文本。
     */
    @SuppressWarnings("unchecked")
    private String formatReport(String aiJson) {
        if (aiJson == null) return buildFallbackReport();

        StringBuilder report = new StringBuilder();
        report.append("═══════════════════════════════════════\n");
        report.append("🤖 AI 测试报告分析\n");
        report.append("═══════════════════════════════════════\n\n");

        try {
            Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(aiJson, Map.class);

            if (data.containsKey("summary")) {
                report.append("📊 ").append(data.get("summary")).append("\n\n");
            }

            if (data.containsKey("highlights")) {
                List<String> highlights = (List<String>) data.get("highlights");
                if (!highlights.isEmpty()) {
                    report.append("✨ 亮点:\n");
                    for (String h : highlights) report.append("  • ").append(h).append("\n");
                    report.append("\n");
                }
            }

            if (data.containsKey("risks")) {
                List<String> risks = (List<String>) data.get("risks");
                if (!risks.isEmpty()) {
                    report.append("⚠️ 风险:\n");
                    for (String r : risks) report.append("  • ").append(r).append("\n");
                    report.append("\n");
                }
            }

            if (data.containsKey("flaky_suspects")) {
                List<String> flaky = (List<String>) data.get("flaky_suspects");
                if (!flaky.isEmpty()) {
                    report.append("🔍 疑似不稳定用例:\n");
                    for (String f : flaky) report.append("  • ").append(f).append("\n");
                    report.append("\n");
                }
            }

            if (data.containsKey("recommendation")) {
                report.append("💡 建议: ").append(data.get("recommendation")).append("\n");
            }
        } catch (Exception e) {
            report.append("(AI 报告解析失败，显示原始数据)\n");
            report.append(aiJson);
        }

        report.append("\n═══════════════════════════════════════\n");
        return report.toString();
    }

    /**
     * 当 AI 不可用时，生成传统统计报告。
     */
    private String buildFallbackReport() {
        int total = passed.size() + failed.size() + skipped.size();
        double passRate = total > 0 ? (double) passed.size() / total * 100 : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("📊 测试执行报告 (传统模式)\n");
        sb.append("═══════════════════════════════════════\n");
        sb.append(String.format("总计: %d | 通过: %d | 失败: %d | 跳过: %d | 通过率: %.1f%%\n",
                total, passed.size(), failed.size(), skipped.size(), passRate));

        if (!failed.isEmpty()) {
            sb.append("\n❌ 失败用例:\n");
            for (TestRecord f : failed) {
                sb.append(String.format("  • %s - %s: %s\n",
                        f.name, f.errorType, f.errorMessage));
            }
        }
        sb.append("═══════════════════════════════════════\n");
        return sb.toString();
    }

    // ========== 内部类 ==========

    static class TestRecord {
        String name;
        String className;
        long durationMs;
        String errorType;
        String errorMessage;
        String groups;
    }
}
