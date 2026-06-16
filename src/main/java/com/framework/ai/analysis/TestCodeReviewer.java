package com.framework.ai.analysis;

import com.framework.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.framework.ai.client.AIClient;

/**
 * 测试代码智能审查器。
 *
 * 对测试脚本做 AI 驱动的质量审查：
 *   - 发现硬编码等待、缺失断言、脆弱定位器
 *   - 检查 Page Object 模式是否规范
 *   - 评估命名、结构、可维护性
 *
 * 使用方式：
 *   String source = Files.readString(Path.of("LoginTest.java"));
 *   TestCodeReviewer.ReviewResult result = TestCodeReviewer.review(source);
 */
public class TestCodeReviewer {

    private static final String SYSTEM_PROMPT = """
            你是 Java 测试代码审查专家，精通 Selenium + TestNG + Page Object 模式。
            审查测试代码，找出质量问题。
            
            输出 JSON：
            {
              "overall_score": 85,
              "summary": "总体评价（一句中文）",
              "issues": [
                {
                  "severity": "ERROR | WARN | INFO",
                  "category": "HARDCODED_WAIT | MISSING_ASSERT | FRAGILE_LOCATOR | 
                              BAD_NAMING | NO_PAGE_OBJECT | DUPLICATE_CODE | 
                              NO_RETRY | UNHANDLED_EXCEPTION | OTHER",
                  "line_hint": "代码片段（帮助定位）",
                  "description": "问题描述（中文）",
                  "suggestion": "改进建议（中文）"
                }
              ],
              "positives": ["做得好的方面1", "做得好的方面2"],
              "improvements": ["整体改进建议1", "改进建议2"]
            }
            只输出 JSON。""";

    private TestCodeReviewer() {}

    /**
     * 审查测试代码。
     *
     * @param sourceCode 测试类源码
     * @return 审查结果
     */
    public static ReviewResult review(String sourceCode) {
        return review(sourceCode, null);
    }

    /**
     * 审查测试代码，附带额外上下文。
     *
     * @param sourceCode 测试类源码
     * @param context    额外上下文（如 "这是登录模块的冒烟测试"）
     */
    public static ReviewResult review(String sourceCode, String context) {
        if (!AIClient.isReady()) {
            LogUtils.warn(TestCodeReviewer.class, "AI 未启用，无法审查");
            return null;
        }

        long start = System.currentTimeMillis();
        LogUtils.info(TestCodeReviewer.class, "🤖 AI 正在审查测试代码...");

        try {
            StringBuilder msg = new StringBuilder();
            if (context != null && !context.isBlank()) {
                msg.append("上下文: ").append(context).append("\n\n");
            }
            msg.append("## 待审查的测试代码\n```java\n");
            // 限制长度，避免 token 超标
            String truncated = sourceCode.length() > 6000
                    ? sourceCode.substring(0, 6000) + "\n// ... (截断)"
                    : sourceCode;
            msg.append(truncated).append("\n```");

            String aiResult = AIClient.get().chat(SYSTEM_PROMPT, msg.toString());
            if (aiResult == null) return null;

            ReviewResult result = parseReview(aiResult);
            long elapsed = System.currentTimeMillis() - start;
            LogUtils.info(TestCodeReviewer.class, "🤖 审查完成 ({}ms): 总分={}, 问题={}",
                    elapsed, result.overallScore, result.issues.size());

            return result;
        } catch (Exception e) {
            LogUtils.error(TestCodeReviewer.class, "代码审查失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 审查单个测试文件。
     */
    public static ReviewResult reviewFile(String filePath) {
        try {
            String source = java.nio.file.Files.readString(java.nio.file.Path.of(filePath));
            return review(source, "文件: " + filePath);
        } catch (Exception e) {
            LogUtils.error(TestCodeReviewer.class, "读取文件失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 批量审查指定目录下的所有测试文件。
     */
    public static List<BatchReviewEntry> reviewDirectory(String dirPath) {
        List<BatchReviewEntry> results = new ArrayList<>();
        try {
            java.io.File dir = new java.io.File(dirPath);
            java.io.File[] files = dir.listFiles((d, name) ->
                    name.endsWith("Test.java") || name.endsWith("Page.java"));
            if (files == null) return results;

            LogUtils.info(TestCodeReviewer.class, "🤖 批量审查 {} 个文件...", files.length);

            for (java.io.File file : files) {
                try {
                    String source = java.nio.file.Files.readString(file.toPath());
                    ReviewResult result = review(source, "文件: " + file.getName());
                    if (result != null) {
                        results.add(new BatchReviewEntry(file.getName(), result));
                    }
                } catch (Exception e) {
                    LogUtils.warn(TestCodeReviewer.class, "审查 {} 失败: {}", file.getName(), e.getMessage());
                }
            }

            // 按分数排序（最低分在前）
            results.sort((a, b) -> Integer.compare(a.result.overallScore, b.result.overallScore));

        } catch (Exception e) {
            LogUtils.error(TestCodeReviewer.class, "批量审查失败: {}", e.getMessage());
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private static ReviewResult parseReview(String json) {
        ReviewResult result = new ReviewResult();
        try {
            Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);

            Object score = data.get("overall_score");
            result.overallScore = score instanceof Number ? ((Number) score).intValue() : 0;
            result.summary = (String) data.getOrDefault("summary", "");
            result.positives = (List<String>) data.getOrDefault("positives", List.of());

            List<Map<String, Object>> issues = (List<Map<String, Object>>) data.get("issues");
            if (issues != null) {
                for (Map<String, Object> issue : issues) {
                    Issue i = new Issue();
                    i.severity = (String) issue.getOrDefault("severity", "INFO");
                    i.category = (String) issue.getOrDefault("category", "OTHER");
                    i.lineHint = (String) issue.getOrDefault("line_hint", "");
                    i.description = (String) issue.getOrDefault("description", "");
                    i.suggestion = (String) issue.getOrDefault("suggestion", "");
                    result.issues.add(i);
                }
            }

            result.improvements = (List<String>) data.getOrDefault("improvements", List.of());
        } catch (Exception e) {
            result.summary = "审查结果解析失败";
        }
        return result;
    }

    // ========== 数据模型 ==========

    public static class Issue {
        public String severity;      // ERROR / WARN / INFO
        public String category;      // 问题类别
        public String lineHint;      // 代码片段
        public String description;   // 描述
        public String suggestion;    // 建议

        public boolean isError() { return "ERROR".equalsIgnoreCase(severity); }
        public boolean isWarning() { return "WARN".equalsIgnoreCase(severity); }

        @Override
        public String toString() {
            String emoji = switch (severity.toUpperCase()) {
                case "ERROR" -> "🔴";
                case "WARN"  -> "🟡";
                default      -> "🔵";
            };
            return String.format("%s [%s] %s\n   位置: %s\n   建议: %s",
                    emoji, category, description, lineHint, suggestion);
        }
    }

    public static class ReviewResult {
        public int overallScore;
        public String summary;
        public List<Issue> issues = new ArrayList<>();
        public List<String> positives = List.of();
        public List<String> improvements = List.of();

        public String grade() {
            if (overallScore >= 90) return "A";
            if (overallScore >= 75) return "B";
            if (overallScore >= 60) return "C";
            return "D";
        }

        public long errorCount() { return issues.stream().filter(Issue::isError).count(); }
        public long warningCount() { return issues.stream().filter(Issue::isWarning).count(); }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("═══════════════════════════════════\n");
            sb.append(String.format("📊 代码审查报告 | 总分: %d/100 (%s级)\n", overallScore, grade()));
            sb.append("📝 ").append(summary).append("\n\n");

            if (!positives.isEmpty()) {
                sb.append("✅ 优点:\n");
                positives.forEach(p -> sb.append("  • ").append(p).append("\n"));
                sb.append("\n");
            }

            if (!issues.isEmpty()) {
                sb.append(String.format("⚠️ 问题 (%d个):\n", issues.size()));
                for (Issue issue : issues) {
                    sb.append(issue.toString()).append("\n\n");
                }
            }

            if (!improvements.isEmpty()) {
                sb.append("💡 改进建议:\n");
                improvements.forEach(i -> sb.append("  • ").append(i).append("\n"));
            }

            sb.append("═══════════════════════════════════\n");
            return sb.toString();
        }
    }

    public static class BatchReviewEntry {
        public final String fileName;
        public final ReviewResult result;

        public BatchReviewEntry(String fileName, ReviewResult result) {
            this.fileName = fileName;
            this.result = result;
        }

        @Override
        public String toString() {
            return String.format("[%d分] %s - %s", result.overallScore, fileName, result.summary);
        }
    }
}
