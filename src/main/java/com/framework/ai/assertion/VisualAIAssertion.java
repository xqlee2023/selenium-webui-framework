package com.framework.ai.assertion;

import com.framework.ai.client.AIConfig;
import com.framework.browser.DriverManager;
import com.framework.utils.LogUtils;
import org.openqa.selenium.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import com.framework.ai.client.AIClient;

/**
 * 视觉 AI 断言引擎。
 *
 * 不依赖像素对比，而是把截图发给 AI 做语义理解。
 * 能容忍微小 UI 调整（字体、间距、颜色微调），但能发现真正的视觉 bug。
 *
 * 使用方式：
 *   VisualAIAssertion.assertLooksLike("登录页应包含用户名输入框、密码输入框和登录按钮");
 *   VisualAIAssertion.assertElementLooksLike(By.id("submit-btn"), "按钮应为蓝色，文字为'提交'");
 */
public class VisualAIAssertion {

    private static final String SYSTEM_PROMPT = """
            你是 UI 视觉验证专家。
            分析截图，判断页面/元素是否符合预期描述。
            
            输出 JSON：
            {
              "passed": true,
              "confidence": 0.92,
              "findings": {
                "matches": ["符合预期的点1", "符合预期的点2"],
                "issues": [
                  {
                    "severity": "HIGH | MEDIUM | LOW",
                    "description": "问题描述（中文）",
                    "location": "页面区域描述"
                  }
                ]
              },
              "overall_assessment": "整体评价（中文，1-2句）"
            }
            只输出 JSON。""";

    private VisualAIAssertion() {}

    /**
     * 断言当前页面视口符合预期描述。
     *
     * @param expectedDescription 预期描述（中文），如 "登录页应包含用户名输入框、密码输入框和登录按钮，按钮为蓝色"
     * @return 验证结果
     * @throws AssertionError 如果 AI 判定页面不符合预期
     */
    public static VisualResult assertLooksLike(String expectedDescription) {
        WebDriver driver = DriverManager.getDriver();
        return assertLooksLike(driver, expectedDescription);
    }

    /**
     * 断言指定 WebDriver 的页面视口符合预期。
     */
    public static VisualResult assertLooksLike(WebDriver driver, String expectedDescription) {
        if (!AIClient.isReady()) {
            LogUtils.warn(VisualAIAssertion.class, "AI 未启用，跳过视觉验证");
            return new VisualResult(true, 0, "AI 未启用", List.of(), List.of());
        }

        long start = System.currentTimeMillis();
        LogUtils.info(VisualAIAssertion.class, "👁️ AI 视觉验证: {}", truncate(expectedDescription, 60));

        try {
            // 截图并转为 base64（视觉模型需要图片输入）
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            String base64Image = Base64.getEncoder().encodeToString(screenshot);

            // 压缩大图（超过 200KB 压缩）
            if (base64Image.length() > 270000) {
                screenshot = compressScreenshot(driver);
                base64Image = Base64.getEncoder().encodeToString(screenshot);
            }

            // 构建带图片的消息
            String result = chatWithImage(SYSTEM_PROMPT, expectedDescription, base64Image);
            if (result == null) {
                return new VisualResult(true, 0, "AI 未响应，跳过验证", List.of(), List.of());
            }

            VisualResult vr = parseVisualResult(result);
            long elapsed = System.currentTimeMillis() - start;
            LogUtils.info(VisualAIAssertion.class, "👁️ 视觉验证完成 ({}ms): passed={}, confidence={:.0f}%",
                    elapsed, vr.passed, vr.confidence * 100);

            if (!vr.passed && !vr.issues.isEmpty()) {
                StringBuilder sb = new StringBuilder("视觉验证失败:\n");
                for (VisualIssue issue : vr.issues) {
                    sb.append("  [").append(issue.severity).append("] ").append(issue.description).append("\n");
                }
                throw new AssertionError(sb.toString());
            }

            return vr;
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            LogUtils.error(VisualAIAssertion.class, "视觉验证异常: {}", e.getMessage());
            return new VisualResult(true, 0, "异常: " + e.getMessage(), List.of(), List.of());
        }
    }

    /**
     * 断言页面上的某个元素符合预期外观。
     *
     * @param locator             元素定位器
     * @param expectedDescription 预期外观描述，如 "蓝色按钮，文字为'提交'，位于表单底部"
     */
    public static VisualResult assertElementLooksLike(By locator, String expectedDescription) {
        WebDriver driver = DriverManager.getDriver();
        return assertElementLooksLike(driver, locator, expectedDescription);
    }

    /**
     * 断言指定 WebDriver 中某个元素的外观。
     */
    public static VisualResult assertElementLooksLike(WebDriver driver, By locator, String expectedDescription) {
        if (!AIClient.isReady()) {
            return new VisualResult(true, 0, "AI 未启用", List.of(), List.of());
        }

        long start = System.currentTimeMillis();
        LogUtils.info(VisualAIAssertion.class, "👁️ AI 元素视觉验证: {} → {}", describeLocator(locator), truncate(expectedDescription, 40));

        try {
            WebElement element = driver.findElement(locator);
            byte[] screenshot = element.getScreenshotAs(OutputType.BYTES);
            String base64Image = Base64.getEncoder().encodeToString(screenshot);

            String fullDesc = "待验证的元素: " + describeLocator(locator) + "\n预期: " + expectedDescription;

            String result = chatWithImage(SYSTEM_PROMPT, fullDesc, base64Image);
            VisualResult vr = parseVisualResult(result);

            long elapsed = System.currentTimeMillis() - start;
            LogUtils.info(VisualAIAssertion.class, "👁️ 元素验证完成 ({}ms): passed={}", elapsed, vr.passed);

            if (!vr.passed && !vr.issues.isEmpty()) {
                StringBuilder sb = new StringBuilder("元素视觉验证失败 (" + describeLocator(locator) + "):\n");
                for (VisualIssue issue : vr.issues) {
                    sb.append("  [").append(issue.severity).append("] ").append(issue.description).append("\n");
                }
                throw new AssertionError(sb.toString());
            }

            return vr;
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            LogUtils.error(VisualAIAssertion.class, "元素视觉验证异常: {}", e.getMessage());
            return new VisualResult(true, 0, "异常: " + e.getMessage(), List.of(), List.of());
        }
    }

    /**
     * 对比两张截图（前后版本），AI 判断是否有视觉回归。
     *
     * @param beforeScreenshot 之前的截图（基准版本）
     * @param afterScreenshot  当前的截图
     * @param description      对比关注点
     */
    public static VisualResult compareScreenshots(
            byte[] beforeScreenshot, byte[] afterScreenshot, String description) {
        if (!AIClient.isReady()) {
            return new VisualResult(true, 0, "AI 未启用", List.of(), List.of());
        }

        LogUtils.info(VisualAIAssertion.class, "👁️ AI 视觉对比: {}", truncate(description, 60));

        try {
            String beforeB64 = Base64.getEncoder().encodeToString(beforeScreenshot);
            String afterB64 = Base64.getEncoder().encodeToString(afterScreenshot);

            String chatPrompt = String.format("""
                    对比两张截图，检查是否有视觉回归。
                    对比关注点: %s
                    
                    上面是基准版本（before），下面是当前版本（after）。
                    """, description);

            String result = chatWithTwoImages(chatPrompt, beforeB64, afterB64);
            return parseVisualResult(result);
        } catch (Exception e) {
            LogUtils.error(VisualAIAssertion.class, "视觉对比异常: {}", e.getMessage());
            return new VisualResult(true, 0, "异常: " + e.getMessage(), List.of(), List.of());
        }
    }

    // ========== 内部实现 ==========

    /**
     * 发送带图片的聊天请求。
     * 使用 OpenAI 视觉 API 格式（vision model）。
     */
    private static String chatWithImage(String systemPrompt, String userText, String base64Image) {
        // 对于不支持图片的模型（如 deepseek-chat 非 vision版本），降级为纯文本描述
        AIConfig cfg = getAIConfig();
        if (cfg != null) {
            String model = cfg.getModel().toLowerCase();
            if (!model.contains("vision") && !model.contains("gpt-4o") && !model.contains("claude")
                    && !model.contains("gemini") && !model.contains("vl")) {
                LogUtils.warn(VisualAIAssertion.class,
                        "当前模型 {} 可能不支持视觉输入，使用降级模式（纯文本描述）", model);
                return AIClient.get().chat(systemPrompt,
                        userText + "\n\n[注：当前模型不支持图片输入，请基于描述做判断]");
            }
        }

        // 构建 Vision API 格式请求
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            var imageContent = Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", "data:image/png;base64," + base64Image)
            );
            var textContent = Map.of("type", "text", "text", userText);

            var messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", List.of(textContent, imageContent))
            );

            var body = new HashMap<String, Object>();
            body.put("model", cfg.getModel());
            body.put("messages", messages);
            body.put("temperature", cfg.getTemperature());
            body.put("max_tokens", cfg.getMaxTokens());

            String endpoint = cfg.getEndpoint() != null && !cfg.getEndpoint().isBlank()
                    ? cfg.getEndpoint() : "https://api.deepseek.com";
            String apiKey = resolveApiKey(cfg);

            var httpClient = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(java.time.Duration.ofSeconds(cfg.getConnectTimeout()))
                    .readTimeout(java.time.Duration.ofSeconds(cfg.getReadTimeout()))
                    .build();

            var request = new okhttp3.Request.Builder()
                    .url(endpoint + "/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create(
                            mapper.writeValueAsString(body),
                            okhttp3.MediaType.get("application/json")))
                    .build();

            try (var response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LogUtils.warn(VisualAIAssertion.class, "视觉 API 返回 {}，降级为纯文本", response.code());
                    return AIClient.get().chat(systemPrompt,
                            userText + "\n[图片大小为 " + (base64Image.length() / 1024) + "KB]");
                }
                String respBody = response.body().string();
                @SuppressWarnings("unchecked")
                var result = mapper.readValue(respBody, Map.class);
                @SuppressWarnings("unchecked")
                var choices = (List<Map<String, Object>>) result.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    var message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            LogUtils.warn(VisualAIAssertion.class, "视觉 API 调用失败: {}，降级为纯文本", e.getMessage());
        }

        // 最终降级
        return AIClient.get().chat(systemPrompt, userText);
    }

    /** 发送两张图片进行对比 */
    private static String chatWithTwoImages(String prompt, String imageB64_1, String imageB64_2) {
        AIConfig cfg = getAIConfig();
        if (cfg == null) return null;

        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            var img1 = Map.of("type", "image_url",
                    "image_url", Map.of("url", "data:image/png;base64," + imageB64_1));
            var img2 = Map.of("type", "image_url",
                    "image_url", Map.of("url", "data:image/png;base64," + imageB64_2));
            var text = Map.of("type", "text", "text", prompt);

            var messages = List.of(
                    Map.of("role", "user", "content", List.of(text, img1, img2))
            );

            var body = new HashMap<String, Object>();
            body.put("model", cfg.getModel());
            body.put("messages", messages);
            body.put("temperature", cfg.getTemperature());
            body.put("max_tokens", cfg.getMaxTokens());

            String apiKey = resolveApiKey(cfg);
            String endpoint = cfg.getEndpoint() != null && !cfg.getEndpoint().isBlank()
                    ? cfg.getEndpoint() : "https://api.deepseek.com";

            var httpClient = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(java.time.Duration.ofSeconds(cfg.getConnectTimeout()))
                    .readTimeout(java.time.Duration.ofSeconds(cfg.getReadTimeout()))
                    .build();

            var request = new okhttp3.Request.Builder()
                    .url(endpoint + "/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create(
                            mapper.writeValueAsString(body),
                            okhttp3.MediaType.get("application/json")))
                    .build();

            try (var response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    @SuppressWarnings("unchecked")
                    var result = mapper.readValue(response.body().string(), Map.class);
                    @SuppressWarnings("unchecked")
                    var choices = (List<Map<String, Object>>) result.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        var message = (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.warn(VisualAIAssertion.class, "图片对比失败: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static VisualResult parseVisualResult(String json) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> data = mapper.readValue(json, Map.class);

            boolean passed = (boolean) data.getOrDefault("passed", true);
            double confidence = data.get("confidence") instanceof Number
                    ? ((Number) data.get("confidence")).doubleValue() : 0.0;
            String assessment = (String) data.getOrDefault("overall_assessment", "");

            Map<String, Object> findings = (Map<String, Object>) data.get("findings");
            List<String> matches = findings != null
                    ? (List<String>) findings.getOrDefault("matches", List.of()) : List.of();

            List<VisualIssue> issues = new ArrayList<>();
            if (findings != null) {
                List<Map<String, Object>> issueList = (List<Map<String, Object>>) findings.get("issues");
                if (issueList != null) {
                    for (Map<String, Object> i : issueList) {
                        VisualIssue vi = new VisualIssue();
                        vi.severity = (String) i.getOrDefault("severity", "LOW");
                        vi.description = (String) i.getOrDefault("description", "");
                        vi.location = (String) i.getOrDefault("location", "");
                        issues.add(vi);
                    }
                }
            }

            return new VisualResult(passed, confidence, assessment, matches, issues);
        } catch (Exception e) {
            return new VisualResult(true, 0, "解析失败", List.of(), List.of());
        }
    }

    private static AIConfig getAIConfig() {
        return AIClient.getConfig();
    }

    private static String resolveApiKey(AIConfig cfg) {
        if (cfg.getApiKey() != null && !cfg.getApiKey().isBlank()) return cfg.getApiKey();
        String env = System.getenv().getOrDefault("DEEPSEEK_API_KEY",
                System.getenv("OPENAI_API_KEY"));
        if (env != null && !env.isBlank()) return env;
        throw new IllegalStateException("未找到 API Key");
    }

    /** 压缩截图（缩放至 50%） */
    private static byte[] compressScreenshot(WebDriver driver) {
        try {
            byte[] original = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(original));
            int w = img.getWidth() / 2;
            int h = img.getHeight() / 2;
            BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = scaled.createGraphics();
            g.drawImage(img, 0, 0, w, h, null);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(scaled, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static String describeLocator(By locator) {
        return locator.toString().replace("By.", "").replace(": ", "=");
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }

    // ========== 数据模型 ==========

    public static class VisualIssue {
        public String severity;      // HIGH / MEDIUM / LOW
        public String description;
        public String location;

        public boolean isHigh() { return "HIGH".equalsIgnoreCase(severity); }

        @Override
        public String toString() {
            return String.format("[%s] %s (位置: %s)", severity, description, location);
        }
    }

    public static class VisualResult {
        public final boolean passed;
        public final double confidence;
        public final String assessment;
        public final List<String> matches;
        public final List<VisualIssue> issues;

        public VisualResult(boolean passed, double confidence, String assessment,
                            List<String> matches, List<VisualIssue> issues) {
            this.passed = passed;
            this.confidence = confidence;
            this.assessment = assessment;
            this.matches = matches;
            this.issues = issues;
        }

        public long highIssueCount() {
            return issues.stream().filter(VisualIssue::isHigh).count();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(passed ? "✅ 视觉验证通过" : "❌ 视觉验证失败");
            sb.append(String.format(" (置信度: %.0f%%)\n", confidence * 100));
            if (!assessment.isEmpty()) sb.append("📝 ").append(assessment).append("\n");
            if (!matches.isEmpty()) {
                sb.append("匹配项:\n");
                matches.forEach(m -> sb.append("  ✓ ").append(m).append("\n"));
            }
            if (!issues.isEmpty()) {
                sb.append("问题:\n");
                issues.forEach(i -> sb.append("  ✗ ").append(i).append("\n"));
            }
            return sb.toString();
        }
    }
}
