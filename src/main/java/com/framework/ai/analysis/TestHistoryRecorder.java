package com.framework.ai.analysis;

import com.framework.utils.LogUtils;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.framework.ai.client.AIClient;

/**
 * 测试执行历史记录器。
 *
 * 每次测试执行时记录结果到本地 JSON，用于后续 Flaky Test 分析。
 * 数据按天分文件存储，自动管理。
 *
 * 线程安全，适合并行执行场景。
 */
public class TestHistoryRecorder {

    private static final String HISTORY_DIR = "test-history";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static volatile TestHistoryRecorder instance;
    private final Path historyDir;
    private final Map<String, List<TestRun>> buffer = new ConcurrentHashMap<>();

    private TestHistoryRecorder() {
        this.historyDir = Paths.get(HISTORY_DIR);
        try { Files.createDirectories(historyDir); } catch (Exception ignored) {}
    }

    public static TestHistoryRecorder get() {
        if (instance == null) {
            synchronized (TestHistoryRecorder.class) {
                if (instance == null) instance = new TestHistoryRecorder();
            }
        }
        return instance;
    }

    /**
     * 记录一次测试运行结果。
     */
    public void record(ITestResult result) {
        TestRun run = new TestRun();
        run.testName = result.getName();
        run.className = result.getTestClass().getName();
        run.status = statusName(result.getStatus());
        run.durationMs = result.getEndMillis() - result.getStartMillis();
        run.timestamp = Instant.ofEpochMilli(result.getStartMillis());
        run.threadId = Thread.currentThread().getName();

        if (result.getThrowable() != null) {
            run.errorType = result.getThrowable().getClass().getSimpleName();
            run.errorMessage = truncate(result.getThrowable().getMessage(), 200);
        }

        String[] groups = result.getMethod().getGroups();
        run.groups = groups.length > 0 ? String.join(",", groups) : "none";

        String key = todayKey();
        buffer.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(run);
    }

    /**
     * 将所有缓冲数据刷到磁盘。建议在 Suite 结束时调用。
     */
    public void flush() {
        if (buffer.isEmpty()) return;

        for (var entry : buffer.entrySet()) {
            Path file = historyDir.resolve("test-runs-" + entry.getKey() + ".jsonl");
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                StringBuilder sb = new StringBuilder();
                for (TestRun run : entry.getValue()) {
                    sb.append(mapper.writeValueAsString(run)).append("\n");
                }
                Files.writeString(file, sb.toString(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                LogUtils.info(getClass(), "📝 已记录 {} 条测试运行记录 → {}", entry.getValue().size(), file.getFileName());
            } catch (IOException e) {
                LogUtils.warn(getClass(), "写入历史记录失败: {}", e.getMessage());
            }
        }
        buffer.clear();
    }

    /**
     * 读取指定日期范围的历史记录。
     */
    public List<TestRun> loadHistory(int daysBack) {
        List<TestRun> all = new ArrayList<>();
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            for (int i = 0; i < daysBack; i++) {
                String date = LocalDateTime.now().minusDays(i).format(DATE_FMT);
                Path file = historyDir.resolve("test-runs-" + date + ".jsonl");
                if (Files.exists(file)) {
                    for (String line : Files.readAllLines(file)) {
                        if (!line.isBlank()) {
                            try {
                                all.add(mapper.readValue(line, TestRun.class));
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (IOException e) {
            LogUtils.warn(getClass(), "读取历史记录失败: {}", e.getMessage());
        }
        return all;
    }

    private String todayKey() { return LocalDateTime.now().format(DATE_FMT); }

    private String statusName(int status) {
        return switch (status) {
            case ITestResult.SUCCESS          -> "PASSED";
            case ITestResult.FAILURE          -> "FAILED";
            case ITestResult.SKIP             -> "SKIPPED";
            default                           -> "UNKNOWN";
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    // ========== 数据模型 ==========

    public static class TestRun {
        public String testName;
        public String className;
        public String status;        // PASSED / FAILED / SKIPPED
        public long durationMs;
        public Instant timestamp;
        public String threadId;
        public String errorType;
        public String errorMessage;
        public String groups;

        public String fullName() { return className + "." + testName; }
        public boolean isPassed() { return "PASSED".equals(status); }
        public boolean isFailed() { return "FAILED".equals(status); }
    }

    /**
     * 统计信息：某个测试多次运行的成功/失败比例。
     */
    public static class TestStats {
        public String fullName;
        public int totalRuns;
        public int passed;
        public int failed;
        public int skipped;
        public double passRate;
        public long avgDurationMs;
        public List<String> recentErrors = new ArrayList<>();
        public boolean isFlaky;  // 标记：是否被判定为不稳定
        public double flakyScore; // 不稳定分数 0~1
    }
}
