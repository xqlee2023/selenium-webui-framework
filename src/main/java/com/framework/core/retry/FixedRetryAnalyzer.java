package com.framework.core.retry;

import com.framework.core.ConfigManager;
import com.framework.utils.LogUtils;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * 固定次数重试分析器（向后兼容）。
 * 当 adaptiveRetry=false 时使用，行为与旧版 RetryAnalyzer 一致。
 */
public class FixedRetryAnalyzer implements IRetryAnalyzer {

    private int retryCount = 0;
    private final int maxRetryCount = ConfigManager.get().retryCount();

    @Override
    public boolean retry(ITestResult result) {
        if (ConfigManager.get().retryOnFailure() && retryCount < maxRetryCount) {
            retryCount++;
            LogUtils.warn(getClass(), "Retrying test '{}' | attempt {}/{}",
                    result.getName(), retryCount, maxRetryCount);
            return true;
        }
        return false;
    }

    public int getRetryCount() { return retryCount; }
}
