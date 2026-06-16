package com.framework.core.retry;

import com.framework.core.ConfigManager;
import com.framework.utils.LogUtils;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ===========================================
 * 🔄 自适应重试分析器 (AdaptiveRetryAnalyzer)
 * ===========================================
 *
 * 超越简单的固定次数重试，根据历史失败模式智能调整重试策略。
 *
 * 核心能力：
 *   1. 按测试方法跟踪失败历史
 *   2. 高频失败的测试增加重试次数
 *   3. 连续通过的测试减少重试次数（节省时间）
 *   4. 特定异常类型智能降级（如 NoSuchElementException 比 AssertionError 更值得重试）
 *   5. 重试间隔指数退避
 *
 * 设计原则：
 *   - 基于配置的初始值，自适应调整
 *   - 线程安全，支持并行执行
 *   - 零耦合：不依赖外部存储，纯内存计算
 *
 * 配置方式：
 *   reporting:
 *     retryOnFailure: true
 *     retryCount: 2          # 基础重试次数
 *     adaptiveRetry: true    # 自适应开关
 *     maxRetryCount: 5       # 最大重试次数上限
 */
public class AdaptiveRetryAnalyzer implements IRetryAnalyzer {

    /** 按测试方法追踪重试次数 */
    private final ConcurrentHashMap<String, AtomicInteger> retryCounters = new ConcurrentHashMap<>();

    /** 按测试方法追踪历史结果 */
    private static final ConcurrentHashMap<String, HistoryEntry> TEST_HISTORY = new ConcurrentHashMap<>();

    private final int baseRetryCount;
    private final int maxRetryCount;

    public AdaptiveRetryAnalyzer() {
        ConfigManager cfg = ConfigManager.get();
        this.baseRetryCount = cfg.retryCount();
        this.maxRetryCount = Math.max(baseRetryCount, 5); // 动态上限
    }

    @Override
    public boolean retry(ITestResult result) {
        String testKey = buildTestKey(result);

        AtomicInteger counter = retryCounters.computeIfAbsent(testKey, k -> new AtomicInteger(0));
        int attempt = counter.getAndIncrement();

        // 计算动态最大重试次数
        int dynamicMaxRetries = calculateDynamicRetryCount(result, testKey);

        if (attempt >= dynamicMaxRetries) {
            // 重试耗尽，记录本次结果
            recordResult(testKey, false);
            retryCounters.remove(testKey);
            return false;
        }

        // 判断重试价值
        if (!shouldRetry(result)) {
            LogUtils.info(getClass(), "⏭️ 跳过重试 (不可恢复错误): {}", testKey);
            recordResult(testKey, false);
            retryCounters.remove(testKey);
            return false;
        }

        // 指数退避等待
        long backoffMs = calculateBackoff(attempt);
        if (backoffMs > 0) {
            try {
                LogUtils.info(getClass(), "⏳ 重试前等待 {}ms (第{}次)", backoffMs, attempt + 1);
                Thread.sleep(backoffMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        LogUtils.info(getClass(), "🔄 第{}次重试: {} (动态最大: {})",
                attempt + 1, testKey, dynamicMaxRetries);
        return true;
    }

    /**
     * 动态计算最大重试次数。
     * 历史失败多的测试获得更多重试机会。
     */
    private int calculateDynamicRetryCount(ITestResult result, String testKey) {
        HistoryEntry history = TEST_HISTORY.get(testKey);

        if (history == null) {
            // 首次遇到，使用基础配置
            return baseRetryCount;
        }

        // 过去 5 次执行中失败率 > 60% → 增加重试
        double failureRate = history.getFailureRate();
        if (failureRate > 0.6) {
            return Math.min(maxRetryCount, baseRetryCount + 3);
        } else if (failureRate > 0.3) {
            return Math.min(maxRetryCount, baseRetryCount + 1);
        }

        // 稳定通过的测试，减少重试次数加速运行
        if (history.consecutiveSuccesses >= 5) {
            return Math.max(0, baseRetryCount - 1);
        }

        return baseRetryCount;
    }

    /**
     * 判断是否值得重试。
     * 某些错误类型重试无意义（如断言失败），直接跳过。
     */
    private boolean shouldRetry(ITestResult result) {
        Throwable t = result.getThrowable();
        if (t == null) return false;

        Class<?> cause = t.getClass();

        // 不可恢复的错误 → 不重试
        if (cause == AssertionError.class ||
            t.getMessage() != null && (
                t.getMessage().contains("assert") &&
                t.getMessage().contains("expected"))) {
            return false;
        }

        // 可恢复的错误 → 值得重试
        if (cause.getSimpleName().contains("TimeoutException") ||
            cause.getSimpleName().contains("StaleElementReferenceException") ||
            cause.getSimpleName().contains("NoSuchElementException") ||
            cause.getSimpleName().contains("WebDriverException") ||
            cause.getSimpleName().contains("ElementClickInterceptedException")) {
            return true;
        }

        // 默认：配置允许就重试
        return true;
    }

    /**
     * 指数退避计算。
     * 第0次重试：500ms
     * 第1次重试：1000ms
     * 第2次重试：2000ms
     * 以此类推，上限 10000ms
     */
    private long calculateBackoff(int attempt) {
        return Math.min(500L * (long) Math.pow(2, attempt), 10000L);
    }

    private String buildTestKey(ITestResult result) {
        return result.getTestClass().getName() + "#" + result.getName();
    }

    private void recordResult(String testKey, boolean success) {
        TEST_HISTORY.compute(testKey, (key, entry) -> {
            if (entry == null) {
                return new HistoryEntry(success);
            }
            entry.record(success);
            return entry;
        });
    }

    /** 重置所有历史记录（通常在 BeforeSuite 中调用）。 */
    public static void resetHistory() {
        TEST_HISTORY.clear();
        LogUtils.info(AdaptiveRetryAnalyzer.class, "自适应重试历史已重置");
    }

    // ========== 内部类 ==========

    static class HistoryEntry {
        private final ConcurrentHashMap<Integer, Boolean> recentResults = new ConcurrentHashMap<>();
        private final AtomicInteger counter = new AtomicInteger(0);
        private int consecutiveSuccesses = 0;
        private int consecutiveFailures = 0;

        HistoryEntry(boolean firstResult) {
            record(firstResult);
        }

        void record(boolean success) {
            int idx = counter.getAndIncrement() % 10; // 保留最近 10 次
            recentResults.put(idx, success);

            if (success) {
                consecutiveSuccesses++;
                consecutiveFailures = 0;
            } else {
                consecutiveFailures++;
                consecutiveSuccesses = 0;
            }
        }

        double getFailureRate() {
            if (recentResults.isEmpty()) return 0.0;
            long failures = recentResults.values().stream()
                    .filter(v -> !v)
                    .count();
            return (double) failures / recentResults.size();
        }
    }
}
