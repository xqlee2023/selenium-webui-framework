package com.framework.ai.lifecycle;

import com.framework.ai.client.AIConfig;
import com.framework.ai.client.AIClient;
import com.framework.utils.LogUtils;

/**
 * ===========================================
 * 🤖 AI 生命周期管理器 (AILifecycleManager)
 * ===========================================
 *
 * 单一职责：负责 AI 客户端的初始化与优雅降级。
 * 从 ConfigManager 中拆分出来——配置管理不应该管 AI 能不能启动。
 *
 * 在 BaseTest @BeforeSuite 中调用 init()。
 */
public class AILifecycleManager {

    private static volatile boolean initialized = false;

    private AILifecycleManager() {}

    /**
     * 初始化 AI 客户端。
     * 如果 AI 配置为 disabled 或已初始化，则跳过。
     * 初始化失败时自动降级（关闭 AI 功能标记），不会阻断测试流程。
     *
     * @param aiConfig AI 配置
     */
    public static void init(AIConfig aiConfig) {
        if (!aiConfig.isEnabled()) {
            LogUtils.info(AILifecycleManager.class, "AI 功能未启用，跳过初始化");
            return;
        }
        if (initialized) {
            LogUtils.info(AILifecycleManager.class, "AI 已初始化，跳过重复初始化");
            return;
        }

        try {
            AIClient.init(aiConfig);
            initialized = true;
            LogUtils.info(AILifecycleManager.class, "🤖 AI 客户端已初始化: provider={}, model={}",
                    aiConfig.getProvider(), aiConfig.getModel());
        } catch (Exception e) {
            LogUtils.warn(AILifecycleManager.class, "AI 初始化失败，已降级为禁用模式: {}", e.getMessage());
            aiConfig.setEnabled(false);
        }
    }

    /** 是否已初始化。 */
    public static boolean isInitialized() {
        return initialized;
    }
}
