package com.framework.event;

import org.testng.ITestResult;

/**
 * 测试相关事件的基类。
 */
public abstract class TestEvent {
    private final ITestResult result;
    private final long timestamp;

    protected TestEvent(ITestResult result) {
        this.result = result;
        this.timestamp = System.currentTimeMillis();
    }

    public ITestResult getResult() { return result; }
    public long getTimestamp() { return timestamp; }
    public String getTestName() { return result != null ? result.getName() : "unknown"; }

    /**
     * 测试开始事件。
     */
    public static class Started extends TestEvent {
        public Started(ITestResult result) { super(result); }
    }

    /**
     * 测试成功事件。
     */
    public static class Success extends TestEvent {
        public Success(ITestResult result) { super(result); }
    }

    /**
     * 测试失败事件。
     */
    public static class Failure extends TestEvent {
        private final Throwable throwable;
        public Failure(ITestResult result) {
            super(result);
            this.throwable = result != null ? result.getThrowable() : null;
        }
        public Throwable getThrowable() { return throwable; }
    }

    /**
     * 测试跳过事件。
     */
    public static class Skipped extends TestEvent {
        public Skipped(ITestResult result) { super(result); }
    }
}
