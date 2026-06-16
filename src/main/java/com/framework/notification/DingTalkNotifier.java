package com.framework.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.framework.utils.LogUtils;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * ===========================================
 * 📢 钉钉群机器人通知 (DingTalkNotifier)
 * ===========================================
 *
 * 通过钉钉群 Webhook 发送 Markdown 消息，支持 @ 指定手机号。
 *
 * <h3>Webhook 获取方式</h3>
 * <ol>
 *   <li>钉钉群 → 群设置 → 智能群助手 → 添加机器人</li>
 *   <li>选择「自定义」→ 复制 Webhook URL</li>
 *   <li>设置为环境变量 DINGTALK_WEBHOOK 或写入 notification.yaml</li>
 * </ol>
 *
 * @author Lee
 * @since 3.2.0
 */
public final class DingTalkNotifier {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(10))
            .build();

    private final String webhookUrl;

    public DingTalkNotifier(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    // ══════════ 发送方法 ══════════

    /**
     * 发送 Markdown 消息（不 @ 任何人）。
     */
    public void send(String title, String markdown) {
        send(title, markdown, List.of(), false);
    }

    /**
     * 发送 Markdown 消息并 @ 指定手机号。
     *
     * @param title     消息标题
     * @param markdown  Markdown 正文
     * @param mobiles   要 @ 的手机号列表
     * @param atAll     是否 @ 所有人
     */
    public void send(String title, String markdown, List<String> mobiles, boolean atAll) {
        try {
            Map<String, Object> body = Map.of(
                    "msgtype", "markdown",
                    "markdown", Map.of("title", title, "text", markdown),
                    "at", Map.of("atMobiles", mobiles, "isAtAll", atAll)
            );

            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            JSON.writeValueAsString(body),
                            MediaType.get("application/json")))
                    .build();

            try (Response response = HTTP.newCall(request).execute()) {
                String resp = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    LogUtils.info(getClass(), "✅ 钉钉通知已发送: {}", title);
                } else {
                    LogUtils.warn(getClass(), "钉钉通知失败 {}: {}", response.code(), resp);
                }
            }
        } catch (IOException e) {
            LogUtils.warn(getClass(), "钉钉通知发送异常: {}", e.getMessage());
        }
    }
}
