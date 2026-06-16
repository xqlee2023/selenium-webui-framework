package com.framework.ai.analysis;

import com.framework.utils.LogUtils;

import java.util.*;
import java.util.stream.Collectors;
import com.framework.ai.client.AIClient;

/**
 * Flaky Test 检测与分析器。
 *
 * 两阶段分析：
 *   第一阶段：统计模式识别（本地计算，无需 AI）
 *     - 通过/失败比率
 *     - 错误类型多样性（多种不同错误 → 信号强）
 *     - 执行时间波动
 *     - 失败后自动通过模式（经典 Flaky）
 *
 *   第二阶段：AI 根因分析（可选）
 *     - 对判定为 Flaky 的用例，发送错误历史给 AI 做深层分析
 *
 * 使用方式：
 *   List<FlakyReport> flaky = FlakyTestAnalyzer.analyze(7);  // 分析最近 7 天
 *   flaky.forEach(System.out::println);
 */
public class FlakyTestAnalyzer {

    // Flaky 判定阈值
    private static final int MIN_RUNS = 5;           // 至少跑过 5 次才分析
    private static final double PASS_RATE_MIN = 0.30; // 通过率低于 30% 大概率是真 Bug，不是 Flaky
    private static final double PASS_RATE_MAX = 0.95; // 通过率 95%+ 也不是 Flaky
    private static final int ERROR_TYPE_VARIETY = 2;  // 错误类型 ≥ 2 种 → Flaky 信号强

    private static final String AI_PROMPT = """
            你是测试稳定性分析专家。
            分析以下 Flaky Test 的历史数据，找出不稳定根因。
            
            输出 JSON：
            {
              "root_cause_category": "LOCATOR | TIMING | DATA | ENV | CONCURRENCY | APP_BUG",
              "root_cause": "根因一句话描述（中文）",
              "stabilization_suggestions": ["建议1", "建议2"],
              "severity": "HIGH | MEDIUM | LOW",
              "confidence": 0.85
            }
            只输出 JSON。""";

    private final TestHistoryRecorder recorder;

    public FlakyTestAnalyzer() {
        this.recorder = TestHistoryRecorder.get();
    }

    /**
     * 分析最近 N 天的历史记录，返回 Flaky 报告列表。
     *
     * @param daysBack 回顾天数
     * @return Flaky Test 报告列表，按不稳定分数降序
     */
    public List<FlakyReport> analyze(int daysBack) {
        List<TestHistoryRecorder.TestRun> history = recorder.loadHistory(daysBack);

        if (history.isEmpty()) {
            LogUtils.warn(getClass(), "没有足够的历史数据（回顾{}天）", daysBack);
            return List.of();
        }

        // 按 testName 分组统计
        Map<String, List<TestHistoryRecorder.TestRun>> grouped = history.stream()
                .collect(Collectors.groupingBy(TestHistoryRecorder.TestRun::fullName));

        List<FlakyReport> reports = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            TestHistoryRecorder.TestStats stats = computeStats(entry.getValue());
            if (stats == null) continue; // 不满足最小运行次数

            double flakyScore = calculateFlakyScore(stats);
            if (flakyScore > 0.3) { // 阈值
                FlakyReport report = new FlakyReport();
                report.stats = stats;
                report.flakyScore = flakyScore;
                report.category = categorizeFlaky(stats);
                reports.add(report);
            }
        }

        // 按 flaky 分数降序
        reports.sort((a, b) -> Double.compare(b.flakyScore, a.flakyScore));

        LogUtils.info(getClass(), "🔍 从 {} 个测试中发现 {} 个 Flaky Test", grouped.size(), reports.size());
        return reports;
    }

    /**
     * 对已发现的 Flaky Test 进行 AI 根因分析。
     */
    public void diagnoseWithAI(FlakyReport report) {
        if (!AIClient.isReady()) return;
        if (report.stats == null) return;

        LogUtils.info(getClass(), "🤖 AI 分析 Flaky 根因: {}", report.stats.fullName);

        try {
            StringBuilder msg = new StringBuilder();
            msg.append("测试: ").append(report.stats.fullName).append("\n");
            msg.append("总计运行: ").append(report.stats.totalRuns).append(" 次\n");
            msg.append("通过: ").append(report.stats.passed)
               .append(" | 失败: ").append(report.stats.failed)
               .append(" | 通过率: ").append(String.format("%.0f%%", report.stats.passRate * 100)).append("\n");
            msg.append("平均耗时: ").append(report.stats.avgDurationMs).append("ms\n");

            if (!report.stats.recentErrors.isEmpty()) {
                msg.append("最近的错误:\n");
                for (String err : report.stats.recentErrors) {
                    msg.append("  - ").append(err).append("\n");
                }
            }

            String aiResult = AIClient.get().chat(AI_PROMPT, msg.toString());
            if (aiResult != null) {
                parseAIDiagnosis(report, aiResult);
                LogUtils.info(getClass(), "✅ Flaky 诊断完成: {}", report.rootCause);
            }
        } catch (Exception e) {
            LogUtils.warn(getClass(), "AI Flaky 分析失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void parseAIDiagnosis(FlakyReport report, String json) {
        try {
            Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
            report.rootCauseCategory = (String) data.get("root_cause_category");
            report.rootCause = (String) data.get("root_cause");
            report.suggestions = (List<String>) data.get("stabilization_suggestions");
            report.severity = (String) data.get("severity");
            Object conf = data.get("confidence");
            report.aiConfidence = conf instanceof Number ? ((Number) conf).doubleValue() : 0.0;
        } catch (Exception e) {
            report.rootCause = "AI 诊断解析失败";
        }
    }

    // ========== 内部算法 ==========

    /**
     * 计算历史统计。
     */
    private TestHistoryRecorder.TestStats computeStats(List<TestHistoryRecorder.TestRun> runs) {
        if (runs.size() < MIN_RUNS) return null; // 数据太少，不分析

        TestHistoryRecorder.TestStats stats = new TestHistoryRecorder.TestStats();
        stats.fullName = runs.get(0).fullName();
        stats.totalRuns = runs.size();

        long totalDuration = 0;
        Set<String> errorTypes = new LinkedHashSet<>();
        List<String> recentErrors = new ArrayList<>();

        for (TestHistoryRecorder.TestRun run : runs) {
            switch (run.status) {
                case "PASSED" -> stats.passed++;
                case "FAILED" -> {
                    stats.failed++;
                    if (run.errorType != null) errorTypes.add(run.errorType);
                    if (run.errorMessage != null && recentErrors.size() < 5) {
                        recentErrors.add(run.errorMessage);
                    }
                }
                case "SKIPPED" -> stats.skipped++;
            }
            totalDuration += run.durationMs;
        }

        stats.passRate = (double) stats.passed / stats.totalRuns;
        stats.avgDurationMs = totalDuration / stats.totalRuns;
        stats.recentErrors = recentErrors;

        return stats;
    }

    /**
     * 计算 Flaky 分数（0~1，越高越可疑）。
     *
     * 算法考量：
     *   - 通过率在 30%~95% 之间（既不是总失败也不是总通过）
     *   - 有多种不同错误类型（不是同一 bug）
     *   - 通过率接近 50%（最经典的不稳定模式）
     *   - 有 "通过→失败→通过" 模式
     */
    private double calculateFlakyScore(TestHistoryRecorder.TestStats stats) {
        double score = 0.0;

        // 1. 通过率在中间范围加分
        if (stats.passRate > PASS_RATE_MIN && stats.passRate < PASS_RATE_MAX) {
            // 越接近 50% 越可疑（使用正弦函数）
            double optimality = Math.sin(Math.PI * stats.passRate);
            score += optimality * 0.4;
        }

        // 2. 有多次失败
        if (stats.failed >= 2) {
            score += Math.min(stats.failed * 0.1, 0.3);
        }

        // 3. 错误类型多样（不同原因导致失败 → 强 Flaky 信号）
        Set<String> uniqueErrors = new HashSet<>(stats.recentErrors);
        if (uniqueErrors.size() >= ERROR_TYPE_VARIETY) {
            score += 0.2;
        }

        // 4. 样本量足够大
        if (stats.totalRuns >= 10) {
            score += 0.1;
        }

        return Math.min(score, 1.0);
    }

    /**
     * 根据统计数据对 Flaky 分类。
     */
    private String categorizeFlaky(TestHistoryRecorder.TestStats stats) {
        if (stats.passRate < 0.4) return "高频失败（可能是真实 Bug）";
        if (stats.passRate > 0.9) return "偶发失败（可能是环境波动）";

        // 检查错误类型
        for (String err : stats.recentErrors) {
            String lower = err.toLowerCase();
            if (lower.contains("timeout") || lower.contains("wait")) return "等待/超时问题";
            if (lower.contains("nosuchelement") || lower.contains("stale")) return "定位器不稳定";
            if (lower.contains("assert") || lower.contains("expect")) return "断言不稳定";
            if (lower.contains("connection") || lower.contains("network")) return "网络/环境问题";
        }
        return "不明确（需要 AI 进一步分析）";
    }

    // ========== 结果模型 ==========

    public static class FlakyReport {
        public TestHistoryRecorder.TestStats stats;
        public double flakyScore;
        public String category;
        public String rootCauseCategory;
        public String rootCause;
        public List<String> suggestions;
        public String severity;
        public double aiConfidence;

        public boolean isSerious() { return "HIGH".equalsIgnoreCase(severity); }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("🔴 [%.0f%% Flaky] %s\n", flakyScore * 100, stats.fullName));
            sb.append(String.format("   通过率: %.0f%% (%d/%d) | 分类: %s\n",
                    stats.passRate * 100, stats.passed, stats.totalRuns, category));
            if (rootCause != null) {
                sb.append("   🤖 AI 诊断: ").append(rootCause).append("\n");
                if (suggestions != null && !suggestions.isEmpty()) {
                    sb.append("   💡 建议: ").append(String.join(" | ", suggestions)).append("\n");
                }
            }
            sb.append(String.format("   最近错误: %s\n",
                    stats.recentErrors.isEmpty() ? "无" : stats.recentErrors.get(0)));
            return sb.toString();
        }
    }

    // ========== 便捷静态方法 ==========

    /**
     * 一键分析：统计 + AI 诊断。
     */
    public static List<FlakyReport> analyzeWithAI(int daysBack) {
        FlakyTestAnalyzer analyzer = new FlakyTestAnalyzer();
        List<FlakyReport> reports = analyzer.analyze(daysBack);

        if (AIClient.isReady()) {
            for (FlakyReport report : reports) {
                analyzer.diagnoseWithAI(report);
            }
        }
        return reports;
    }
}
