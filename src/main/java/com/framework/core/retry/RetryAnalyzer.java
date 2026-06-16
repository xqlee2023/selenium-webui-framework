package com.framework.core.retry;

import com.framework.utils.LogUtils;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * ===========================================
 * 🔄 重试分析器 (RetryAnalyzer) — 自适应入口
 * ===========================================
 *
 * 统一重试入口，根据配置动态选择策略：
 *   - adaptiveRetry: true  → 使用 AdaptiveRetryAnalyzer（智能重试）
 *   - adaptiveRetry: false → 使用固定次数重试（向后兼容）
 *
 * v3.2.0 新增：自适应重试模式，根据历史失败模式调整策略。
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private final IRetryAnalyzer delegate;

    public RetryAnalyzer() {
        boolean adaptive = com.framework.core.ConfigManager.get()
                .retryOnFailure() && isAdaptiveEnabled();
        this.delegate = adaptive
                ? new AdaptiveRetryAnalyzer()
                : new FixedRetryAnalyzer();
        LogUtils.debug(getClass(), "重试策略: {}", delegate.getClass().getSimpleName());
    }

    @Override
    public boolean retry(ITestResult result) {
        return delegate.retry(result);
    }

    private boolean isAdaptiveEnabled() {
        try {
            // 通过环境变量或系统属性控制
            String val = System.getProperty("adaptiveRetry");
            if (val != null) return Boolean.parseBoolean(val);
            return true; // 默认开启
        } catch (Exception e) {
            return true;
        }
    }
}