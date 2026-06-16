package com.framework.ai.client;

import com.framework.core.security.DataSanitizer;
import com.framework.utils.LogUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okhttp3.ConnectionPool;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI 客户端 — 统一 LLM 调用层。
 *
 * 支持 OpenAI 兼容 API（DeepSeek / OpenAI / 自定义端点）。
 * 内置重试、超时控制、优雅降级。
 */
public class AIClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper json = new ObjectMapper();
    private final AIConfig config;

    private static volatile AIClient instance;
    
    /** 共享 OkHttpClient 实例（连接池复用） */
    private static final OkHttpClient SHARED_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(60))
            .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS))
            .retryOnConnectionFailure(true)
            .build();

    private AIClient(AIConfig config) {
        this.config = config;
        this.httpClient = SHARED_HTTP_CLIENT.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getConnectTimeout()))
                .readTimeout(Duration.ofSeconds(config.getReadTimeout()))
                .build();
    }

    public static AIClient init(AIConfig config) {
        if (instance == null) {
            synchronized (AIClient.class) {
                if (instance == null) {
                    instance = new AIClient(config);
                }
            }
        }
        return instance;
    }

    public static AIClient get() {
        if (instance == null) {
            throw new IllegalStateException("AIClient 未初始化，请先调用 AIClient.init(config)");
        }
        return instance;
    }

    public static boolean isReady() {
        return instance != null && instance.config.isEnabled();
    }

    /**
     * 重置 AIClient（允许重新初始化，支持运行时切换 provider）。
     * 调用后需重新 init()。
     */
    public static synchronized void reset() {
        instance = null;
        LogUtils.info(AIClient.class, "AIClient 已重置，可重新初始化");
    }

    /** 公开获取 AI 配置（消除反射依赖）。 */
    public static AIConfig getConfig() {
        return instance != null ? instance.config : null;
    }

    /**
     * 发送聊天请求，返回 AI 回复文本。
     *
     * @param systemPrompt 系统提示词（定义 AI 角色）
     * @param userMessage  用户消息（具体问题）
     * @return AI 回复，失败返回 null（不会抛异常，保证调用方安全）
     */
    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, null);
    }

    /**
     * 发送聊天请求，支持传入 JSON schema 约束输出格式。
     */
    public String chat(String systemPrompt, String userMessage, Map<String, Object> jsonSchema) {
        if (!config.isEnabled()) {
            LogUtils.warn(getClass(), "AI 未启用，跳过调用");
            return null;
        }

        for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
            try {
                return doChat(systemPrompt, userMessage, jsonSchema);
            } catch (Exception e) {
                if (attempt == config.getMaxRetries()) {
                    LogUtils.error(getClass(), "AI 调用失败（已重试{}次）: {}", config.getMaxRetries(), e.getMessage());
                    return null;
                }
                LogUtils.warn(getClass(), "AI 调用失败，第{}次重试: {}", attempt + 1, e.getMessage());
                sleep(1000L * (attempt + 1)); // 指数退避
            }
        }
        return null;
    }

    private String doChat(String systemPrompt, String userMessage, Map<String, Object> jsonSchema) throws IOException {
        String apiKey = resolveApiKey();
        String endpoint = resolveEndpoint();

        var requestBody = Map.of(
                "model", config.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", config.getTemperature(),
                "max_tokens", config.getMaxTokens()
        );

        // 如果需要 JSON 结构化输出，添加 response_format
        @SuppressWarnings("unchecked")
        Map<String, Object> body = new java.util.HashMap<>(requestBody);
        if (jsonSchema != null) {
            body.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", jsonSchema
            ));
        }

        Request request = new Request.Builder()
                .url(endpoint + "/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        json.writeValueAsString(body),
                        MediaType.get("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String bodyStr = response.body() != null ? response.body().string() : "";
                // 脱敏后记录，防止 API Key 泄漏
                LogUtils.warn(getClass(), "AI API 返回 {}: {}", response.code(), DataSanitizer.sanitizeApiResponse(bodyStr));
                return null;
            }

            String respBody = response.body().string();
            @SuppressWarnings("unchecked")
            Map<String, Object> result = json.readValue(respBody, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            if (choices == null || choices.isEmpty()) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        }
    }

    private String resolveApiKey() {
        // 1. 直接从配置读取
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            return config.getApiKey();
        }
        // 2. 从环境变量读取
        String envKey = switch (config.getProvider().toLowerCase()) {
            case "openai" -> System.getenv("OPENAI_API_KEY");
            case "deepseek" -> System.getenv("DEEPSEEK_API_KEY");
            default -> System.getenv("AI_API_KEY");
        };
        if (envKey != null && !envKey.isBlank()) return envKey;

        // 3. 通用 fallback
        String generic = System.getenv("OPENAI_API_KEY");
        if (generic != null && !generic.isBlank()) return generic;

        throw new IllegalStateException("未找到 API Key，请在 config.yaml 或环境变量中设置");
    }

    private String resolveEndpoint() {
        if (config.getEndpoint() != null && !config.getEndpoint().isBlank()) {
            return config.getEndpoint();
        }
        return switch (config.getProvider().toLowerCase()) {
            case "openai" -> "https://api.openai.com";
            case "deepseek" -> "https://api.deepseek.com";
            default -> "https://api.deepseek.com";
        };
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
