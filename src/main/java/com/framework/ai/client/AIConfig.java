package com.framework.ai.client;

/**
 * AI 配置 POJO，映射 config.yaml 中的 ai: 节点。
 */
public class AIConfig {

    private boolean enabled = false;
    private String provider = "deepseek";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private String endpoint = "";
    private int maxTokens = 1024;
    private double temperature = 0.3;
    private int connectTimeout = 30;   // seconds
    private int readTimeout = 60;       // seconds
    private int maxRetries = 2;

    // ========== 子配置 ==========
    private FailureAnalysisConfig failureAnalysis = new FailureAnalysisConfig();
    private SelfHealingConfig selfHealing = new SelfHealingConfig();
    private ReportConfig report = new ReportConfig();
    private VisualConfig visual = new VisualConfig();
    private DOMChangeConfig domChange = new DOMChangeConfig();

    // Getters & Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public FailureAnalysisConfig getFailureAnalysis() { return failureAnalysis; }
    public void setFailureAnalysis(FailureAnalysisConfig failureAnalysis) { this.failureAnalysis = failureAnalysis; }
    public SelfHealingConfig getSelfHealing() { return selfHealing; }
    public void setSelfHealing(SelfHealingConfig selfHealing) { this.selfHealing = selfHealing; }
    public ReportConfig getReport() { return report; }
    public void setReport(ReportConfig report) { this.report = report; }
    public VisualConfig getVisual() { return visual; }
    public void setVisual(VisualConfig visual) { this.visual = visual; }
    public DOMChangeConfig getDomChange() { return domChange; }
    public void setDomChange(DOMChangeConfig domChange) { this.domChange = domChange; }

    // ---- 子配置类 ----

    public static class FailureAnalysisConfig {
        private boolean enabled = true;
        private String promptTemplate = "default";
        private boolean includePageSource = true;
        private boolean includeStackTrace = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getPromptTemplate() { return promptTemplate; }
        public void setPromptTemplate(String promptTemplate) { this.promptTemplate = promptTemplate; }
        public boolean isIncludePageSource() { return includePageSource; }
        public void setIncludePageSource(boolean includePageSource) { this.includePageSource = includePageSource; }
        public boolean isIncludeStackTrace() { return includeStackTrace; }
        public void setIncludeStackTrace(boolean includeStackTrace) { this.includeStackTrace = includeStackTrace; }
    }

    public static class SelfHealingConfig {
        private boolean enabled = true;
        private int maxCandidates = 3;
        private int timeoutMs = 5000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxCandidates() { return maxCandidates; }
        public void setMaxCandidates(int maxCandidates) { this.maxCandidates = maxCandidates; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class ReportConfig {
        private boolean enabled = true;
        private boolean verbose = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isVerbose() { return verbose; }
        public void setVerbose(boolean verbose) { this.verbose = verbose; }
    }

    public static class VisualConfig {
        private boolean enabled = true;
        private int maxImageSizeKB = 200;
        private boolean autoCompress = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxImageSizeKB() { return maxImageSizeKB; }
        public void setMaxImageSizeKB(int maxImageSizeKB) { this.maxImageSizeKB = maxImageSizeKB; }
        public boolean isAutoCompress() { return autoCompress; }
        public void setAutoCompress(boolean autoCompress) { this.autoCompress = autoCompress; }
    }

    public static class DOMChangeConfig {
        private boolean enabled = true;
        private String snapshotDir = "dom-snapshots";
        private boolean autoCapture = false;
        private int maxElementsPerSnapshot = 200;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getSnapshotDir() { return snapshotDir; }
        public void setSnapshotDir(String snapshotDir) { this.snapshotDir = snapshotDir; }
        public boolean isAutoCapture() { return autoCapture; }
        public void setAutoCapture(boolean autoCapture) { this.autoCapture = autoCapture; }
        public int getMaxElementsPerSnapshot() { return maxElementsPerSnapshot; }
        public void setMaxElementsPerSnapshot(int maxElementsPerSnapshot) { this.maxElementsPerSnapshot = maxElementsPerSnapshot; }
    }
}
