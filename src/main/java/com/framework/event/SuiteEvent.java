package com.framework.event;

import org.testng.ISuite;

/**
 * Suite 生命周期事件。
 */
public abstract class SuiteEvent {
    private final ISuite suite;
    private final long timestamp;

    protected SuiteEvent(ISuite suite) {
        this.suite = suite;
        this.timestamp = System.currentTimeMillis();
    }

    public ISuite getSuite() { return suite; }
    public long getTimestamp() { return timestamp; }

    /**
     * Suite 开始事件。
     */
    public static class Started extends SuiteEvent {
        public Started(ISuite suite) { super(suite); }
    }

    /**
     * Suite 结束事件。
     */
    public static class Finished extends SuiteEvent {
        public Finished(ISuite suite) { super(suite); }
    }
}
