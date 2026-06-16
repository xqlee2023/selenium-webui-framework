package com.framework.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.framework.utils.LogUtils;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ===========================================
 * 📋 通知配置（从 notification.yaml 加载）
 * ===========================================
 */
public class NotificationConfig {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    public boolean enabled = true;
    public String dingtalkWebhook = "";
    public String reportBaseUrl = "";
    public List<ModuleEntry> modules = List.of();
    public String defaultOwner = "测试负责人";
    public String defaultMobile = "";
    public boolean notifyOnSuccess = true;
    public boolean notifyOnFailure = true;
    public boolean notifyOnUnstable = true;
    public String mentionMode = "per_module";

    /** 模块负责人映射 */
    public record ModuleEntry(
            String key,      // 类名，如 "LoginTest"
            String name,     // 显示名，如 "登录模块"
            String owner,    // 负责人
            String mobile    // 钉钉手机号
    ) {}

    public String displayName;

    public String resolveWebhook() {
        if (dingtalkWebhook != null && !dingtalkWebhook.isBlank()) return dingtalkWebhook;
        String env = System.getenv("DINGTALK_WEBHOOK");
        if (env != null && !env.isBlank()) return env;
        throw new IllegalStateException("未配置钉钉 Webhook，请在 notification.yaml 或环境变量 DINGTALK_WEBHOOK 中设置");
    }

    /**
     * 根据测试类名查找模块负责人。
     * 未匹配到的模块使用默认负责人。
     */
    public ModuleEntry getModuleOwner(String testClassName) {
        return modules.stream()
                .filter(m -> testClassName.equals(m.key))
                .findFirst()
                .map(m -> new ModuleEntry(m.key, m.name, m.owner, m.mobile))
                .orElse(new ModuleEntry(testClassName, testClassName, defaultOwner, defaultMobile));
    }

    /** 加载 notification.yaml */
    public static NotificationConfig load() {
        try (InputStream in = NotificationConfig.class.getClassLoader()
                .getResourceAsStream("config/notification.yaml")) {
            if (in == null) {
                LogUtils.warn(NotificationConfig.class, "notification.yaml 未找到，通知功能禁用");
                NotificationConfig c = new NotificationConfig();
                c.enabled = false;
                return c;
            }
            Map<String, Object> root = YAML.readValue(in, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> notif = (Map<String, Object>) root.get("notification");
            if (notif == null) {
                NotificationConfig c = new NotificationConfig();
                c.enabled = false;
                return c;
            }

            NotificationConfig cfg = new NotificationConfig();
            cfg.enabled = (boolean) notif.getOrDefault("enabled", true);
            cfg.dingtalkWebhook = (String) notif.getOrDefault("dingtalkWebhook", "");
            cfg.reportBaseUrl = (String) notif.getOrDefault("reportBaseUrl", "");
            cfg.defaultOwner = (String) notif.getOrDefault("defaultOwner", "测试负责人");
            cfg.defaultMobile = (String) notif.getOrDefault("defaultMobile", "");
            cfg.notifyOnSuccess = (boolean) notif.getOrDefault("notifyOnSuccess", true);
            cfg.notifyOnFailure = (boolean) notif.getOrDefault("notifyOnFailure", true);
            cfg.notifyOnUnstable = (boolean) notif.getOrDefault("notifyOnUnstable", true);
            cfg.mentionMode = (String) notif.getOrDefault("mentionMode", "per_module");

            // 解析模块列表
            @SuppressWarnings("unchecked")
            Map<String, Object> modulesMap = (Map<String, Object>) notif.get("modules");
            if (modulesMap != null) {
                cfg.modules = modulesMap.entrySet().stream()
                        .map(e -> {
                            @SuppressWarnings("unchecked")
                            Map<String, String> m = (Map<String, String>) e.getValue();
                            return new ModuleEntry(
                                    e.getKey(),
                                    m.getOrDefault("name", e.getKey()),
                                    m.getOrDefault("owner", ""),
                                    m.getOrDefault("mobile", "")
                            );
                        })
                        .toList();
            }

            LogUtils.info(NotificationConfig.class, "📋 通知配置已加载: {} 个模块, 模式={}",
                    cfg.modules.size(), cfg.mentionMode);
            return cfg;
        } catch (Exception e) {
            LogUtils.warn(NotificationConfig.class, "加载 notification.yaml 失败: {}", e.getMessage());
            NotificationConfig c = new NotificationConfig();
            c.enabled = false;
            return c;
        }
    }
}
