package com.framework.ai.analysis;

import com.framework.utils.LogUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import com.framework.ai.client.AIClient;

/**
 * 失败智能诊断器。
 *
 * 测试失败时，把截图 + 异常 + 页面 DOM 发给 LLM，
 * AI 综合分析后给出自然语言诊断结论。
 *
 * 能区分：被测应用 Bug vs 测试脚本问题 vs 环境/数据问题。
 */
public class FailureAnalyzer {

    private static final String SYSTEM_PROMPT = """
            你是一个资深的自动化测试分析专家。
            你的任务：分析 UI 自动化测试失败案例，给出诊断结论。
            
            输出格式（JSON）：
            {
              "root_cause": "一句话根因",
              "category": "BUG | SCRIPT | ENV | DATA | TIMEOUT",
              "severity": "HIGH | MEDIUM | LOW",
              "explanation": "详细分析（中文，2-5句）",
              "fix_suggestion": "修复建议（中文，1-3句）",
              "confidence": 0.85
            }
            
            category 说明：
            - BUG: 被测应用本身有问题（页面报错、数据显示错误、功能异常）
            - SCRIPT: 测试脚本写错了（定位器失效、等待不足、断言错误）
            - ENV: 环境问题（网络超时、服务不可用、浏览器问题）
            - DATA: 测试数据问题（账号过期、数据被污染、权限不足）
            - TIMEOUT: 超时（页面加载慢、元素渲染慢）
            
            只输出 JSON，不要有其他内容。""";

    private FailureAnalyzer() {}

    /**
     * 分析测试失败。
     *
     * @param driver      WebDriver（用于获取截图和页面源码）
     * @param throwable   测试抛出的异常
     * @param testName    测试用例名称
     * @return 诊断结果 JSON 字符串，失败返回 null
     */
    public static String analyze(WebDriver driver, Throwable throwable, String testName) {
        if (!AIClient.isReady()) return null;

        long start = System.currentTimeMillis();
        LogUtils.info(FailureAnalyzer.class, "🤖 AI 正在诊断失败: {}", testName);

        try {
            StringBuilder userMsg = new StringBuilder();
            userMsg.append("测试用例: ").append(testName).append("\n\n");

            // 异常信息
            if (throwable != null) {
                userMsg.append("## 异常信息\n");
                userMsg.append("类型: ").append(throwable.getClass().getName()).append("\n");
                userMsg.append("消息: ").append(throwable.getMessage()).append("\n\n");

                // 只取前 15 行堆栈，避免 token 浪费
                userMsg.append("堆栈（前 15 行）:\n");
                StackTraceElement[] frames = throwable.getStackTrace();
                int limit = Math.min(frames.length, 15);
                for (int i = 0; i < limit; i++) {
                    userMsg.append("  ").append(frames[i].toString()).append("\n");
                }
                userMsg.append("\n");
            }

            // 页面 URL 和标题
            if (driver != null) {
                try {
                    userMsg.append("## 当前页面\n");
                    userMsg.append("URL: ").append(driver.getCurrentUrl()).append("\n");
                    userMsg.append("标题: ").append(driver.getTitle()).append("\n\n");
                } catch (Exception ignored) {}
            }

            // 页面 DOM 摘要
            if (driver != null) {
                try {
                    String text = getPageTextSummary(driver);
                    if (text != null && !text.isBlank()) {
                        userMsg.append("## 页面可见文本（前 500 字符）\n");
                        userMsg.append(text.length() > 500 ? text.substring(0, 500) : text);
                        userMsg.append("\n");
                    }
                } catch (Exception ignored) {}
            }

            String result = AIClient.get().chat(SYSTEM_PROMPT, userMsg.toString());
            long elapsed = System.currentTimeMillis() - start;
            LogUtils.info(FailureAnalyzer.class, "🤖 AI 诊断完成 ({}ms): {}", elapsed,
                    result != null ? result.substring(0, Math.min(100, result.length())) : "无结果");

            return result;
        } catch (Exception e) {
            LogUtils.warn(FailureAnalyzer.class, "AI 诊断异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取页面可见文本摘要（用于让 AI 理解页面内容）。
     */
    private static String getPageTextSummary(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // 获取 body 内的可见文本，排除 script/style
            Object result = js.executeScript(
                    "var body = document.body;" +
                    "if (!body) return '';" +
                    "var clone = body.cloneNode(true);" +
                    "var scripts = clone.querySelectorAll('script, style, noscript, svg, head');" +
                    "scripts.forEach(function(s) { s.remove(); });" +
                    "var text = clone.innerText || clone.textContent || '';" +
                    "return text.replace(/\\n{3,}/g, '\\n\\n').trim().substring(0, 2000);");
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 将诊断结果解析为结构化对象。
     */
    public static FailureResult parseResult(String json) {
        if (json == null) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, FailureResult.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 诊断结果 POJO。
     */
    public static class FailureResult {
        public String root_cause;
        public String category;
        public String severity;
        public String explanation;
        public String fix_suggestion;
        public double confidence;

        public boolean isBug() { return "BUG".equalsIgnoreCase(category); }
        public boolean isScriptIssue() { return "SCRIPT".equalsIgnoreCase(category); }
        public boolean isEnvIssue() { return "ENV".equalsIgnoreCase(category); }

        @Override
        public String toString() {
            return String.format("[%s|%s] %s\n📝 %s\n🔧 %s (置信度: %.0f%%)",
                    category, severity, root_cause, explanation, fix_suggestion, confidence * 100);
        }
    }
}
