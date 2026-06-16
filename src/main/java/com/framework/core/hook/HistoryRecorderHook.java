package com.framework.core.hook;

import com.framework.ai.analysis.TestHistoryRecorder;
import org.testng.ISuite;
import org.testng.ITestResult;

/**
 * 测试历史记录 Hook。
 * 单一职责：记录每次测试运行结果到本地 JSON。
 */
public class HistoryRecorderHook implements TestEventHook {

    private final TestHistoryRecorder recorder = TestHistoryRecorder.get();

    @Override
    public void onTestSuccess(ITestResult result) {
        recorder.record(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        recorder.record(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        recorder.record(result);
    }

    @Override
    public void onSuiteFinish(ISuite suite) {
        recorder.flush();
    }
}
