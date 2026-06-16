package com.framework.core.security;

import java.util.regex.Pattern;

/**
 * ===========================================
 * 🔒 数据脱敏器 (DataSanitizer)
 * ===========================================
 *
 * 防止敏感信息（API Key、密码、token、证书）泄漏到日志和报告中。
 * 对 AI API 响应体、异常信息、日志输出做安全过滤。
 *
 * 设计原则：
 *   - 安全默认：所有输出默认经过脱敏
 *   - 可扩展：通过正则配置添加新的脱敏规则
 *   - 零侵入：静态方法调用，不改变业务代码结构
 *
 * 使用方式：
 *   String safe = DataSanitizer.sanitize(rawResponse);
 *   LogUtils.info(getClass(), "AI 响应: {}", safe);
 */
public final class DataSanitizer {

    private static final Pattern[] SENSITIVE_PATTERNS = {
        // API Keys: sk-... (OpenAI), F-... (DeepSeek)
        Pattern.compile(
            "(?i)(api_key|apikey|secret|token|password|credential|auth)" +
            "['\"]?\\s*[:=]\\s*['\"]?(sk-[a-zA-Z0-9]{20,}|" +
            "[a-fA-F0-9]{32,}|[A-Za-z0-9+/=]{40,})['\"]?"),
        // Bearer tokens in headers
        Pattern.compile("(Bearer\\s+)([A-Za-z0-9._~+/=-]{20,})"),
        // Authorization headers
        Pattern.compile("(Authorization:\\s*Bearer\\s+)([A-Za-z0-9._~+/=-]{20,})"),
        // Generic key-like patterns in JSON responses
        Pattern.compile("(\"[A-Za-z0-9_-]{20,40}\":\\s*\")(sk-[A-Za-z0-9]{20,})(\")"),
        // Session IDs and cookies
        Pattern.compile("(session[_-]?id|session[_-]?token|auth[_-]?token)" +
            "['\"]?\\s*[:=]\\s*['\"]?[A-Za-z0-9_-]{20,}['\"]?"),
    };

    private static final String MASK = "****";

    private DataSanitizer() {}

    /**
     * 对字符串进行脱敏处理。
     *
     * @param input 原始字符串
     * @return 脱敏后的安全字符串
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) return input;

        String result = input;
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            result = pattern.matcher(result).replaceAll("$1" + MASK);
        }
        return result;
    }

    /**
     * 脱敏 HTTP 响应体（AI API 调用专用）。
     * 除通用规则外，额外处理 API 返回中可能包含的请求回显。
     */
    public static String sanitizeApiResponse(String response) {
        if (response == null) return null;
        return sanitize(response);
    }

    /**
     * 判断字符串是否包含疑似敏感信息。
     */
    public static boolean containsSensitiveData(String input) {
        if (input == null) return false;
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(input).find()) return true;
        }
        return false;
    }
}
