package com.framework.ai.healing;

import com.framework.utils.LogUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.framework.ai.client.AIClient;

/**
 * DOM 变更检测与影响分析引擎。
 *
 * 在测试执行时自动捕获页面 DOM 快照，与基准版本对比，
 * 用 AI 分析变更范围并预测受影响的测试用例。
 *
 * 使用场景：
 *   - 开发改了前端代码 → 跑一次测试 → 自动告诉你哪些用例可能受影响
 *   - 页面重构后 → 对比新旧 DOM → 找出定位器风险点
 *
 * 使用方式：
 *   // 1. 建立基准快照
 *   DOMChangeDetector.captureBaseline(driver, "login-page");
 *
 *   // 2. 后续对比
 *   DOMChangeDetector.ChangeReport report = DOMChangeDetector.detect(driver, "login-page");
 *
 *   // 3. AI 影响分析
 *   DOMChangeDetector.ImpactReport impact = DOMChangeDetector.analyzeImpact(report);
 */
public class DOMChangeDetector {

    private static final String SNAPSHOT_DIR = "dom-snapshots";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 关键的 DOM 属性，用于变更检测
    private static final List<String> TRACKED_ATTRIBUTES = List.of(
            "id", "class", "data-testid", "aria-label", "name", "type",
            "placeholder", "role", "href", "alt", "title"
    );

    private static final String AI_IMPACT_PROMPT = """
            你是测试影响分析专家。
            分析 DOM 变更，预测哪些类型的测试用例可能受影响。
            
            输出 JSON：
            {
              "change_summary": "变更摘要（中文，一句话）",
              "risk_level": "HIGH | MEDIUM | LOW",
              "affected_patterns": ["可能受影响的定位器模式1", "模式2"],
              "affected_test_categories": ["登录测试", "导航测试"],
              "specific_recommendations": [
                {
                  "locator": "受影响的定位器",
                  "reason": "受影响原因",
                  "suggested_fix": "建议的新定位器"
                }
              ],
              "safe_zones": ["未受影响的区域1"],
              "estimated_impact_test_count": 5
            }
            只输出 JSON。""";

    private DOMChangeDetector() {}

    /**
     * 捕获页面 DOM 快照作为基准。
     *
     * @param driver   WebDriver
     * @param pageName 页面名称（如 "login"、"dashboard"）
     */
    public static DOMSnapshot captureBaseline(WebDriver driver, String pageName) {
        DOMSnapshot snapshot = capture(driver, pageName);
        saveSnapshot(snapshot, pageName + "-baseline");
        LogUtils.info(DOMChangeDetector.class, "📸 DOM 基准快照已保存: {} ({} 个元素)",
                pageName, snapshot.elements.size());
        return snapshot;
    }

    /**
     * 捕获当前页面的 DOM 快照。
     */
    public static DOMSnapshot capture(WebDriver driver, String pageName) {
        DOMSnapshot snapshot = new DOMSnapshot();
        snapshot.pageName = pageName;
        snapshot.url = getCurrentUrl(driver);
        snapshot.title = getTitle(driver);
        snapshot.capturedAt = LocalDateTime.now().format(FMT);

        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> raw = (List<Map<String, Object>>) js.executeScript(
                    "var elements = document.querySelectorAll('" +
                    "a, button, input, select, textarea, form, " +
                    "[data-testid], [role], [aria-label], " +
                    "h1, h2, h3, h4, label, nav, header, footer, main');" +
                    "return Array.from(elements).map(function(el) {" +
                    "  var info = {tag: el.tagName.toLowerCase()};" +
                    "  if (el.id) info.id = el.id;" +
                    "  if (el.className && typeof el.className === 'string') info.class = el.className;" +
                    "  ['data-testid','aria-label','name','type','placeholder','role','href','title','alt'].forEach(function(a) {" +
                    "    var v = el.getAttribute(a); if (v) info[a] = v;" +
                    "  });" +
                    "  var txt = (el.textContent || '').trim();" +
                    "  if (txt && txt.length < 150) info.text = txt;" +
                    "  return info;" +
                    "});");

            for (Map<String, Object> r : raw) {
                DOMElement e = new DOMElement();
                e.tag = (String) r.get("tag");
                e.id = (String) r.get("id");
                e.className = (String) r.get("class");
                e.dataTestId = (String) r.get("data-testid");
                e.ariaLabel = (String) r.get("aria-label");
                e.name = (String) r.get("name");
                e.type = (String) r.get("type");
                e.placeholder = (String) r.get("placeholder");
                e.role = (String) r.get("role");
                e.href = (String) r.get("href");
                e.title = (String) r.get("title");
                e.text = (String) r.get("text");
                e.computeFingerprint();
                snapshot.elements.add(e);
            }
        } catch (Exception e) {
            LogUtils.warn(DOMChangeDetector.class, "DOM 快照捕获异常: {}", e.getMessage());
        }

        return snapshot;
    }

    /**
     * 与基准快照对比，检测变更。
     *
     * @param driver   当前 WebDriver
     * @param pageName 页面名称
     * @return 变更报告（若无变更返回 null）
     */
    public static ChangeReport detect(WebDriver driver, String pageName) {
        DOMSnapshot current = capture(driver, pageName);
        DOMSnapshot baseline = loadSnapshot(pageName + "-baseline");

        if (baseline == null) {
            LogUtils.warn(DOMChangeDetector.class, "未找到基准快照: {}，请先执行 captureBaseline()", pageName);
            return null;
        }

        return compare(baseline, current);
    }

    /**
     * 对比两个快照。
     */
    public static ChangeReport compare(DOMSnapshot before, DOMSnapshot after) {
        ChangeReport report = new ChangeReport();
        report.before = before;
        report.after = after;

        // 构建指纹索引
        Set<String> beforeFingerprints = before.elements.stream()
                .map(e -> e.fingerprint)
                .collect(Collectors.toSet());
        Set<String> afterFingerprints = after.elements.stream()
                .map(e -> e.fingerprint)
                .collect(Collectors.toSet());

        // 删除的元素
        Set<String> removed = new HashSet<>(beforeFingerprints);
        removed.removeAll(afterFingerprints);

        // 新增的元素
        Set<String> added = new HashSet<>(afterFingerprints);
        added.removeAll(beforeFingerprints);

        // 解析变更细节
        for (DOMElement e : before.elements) {
            if (removed.contains(e.fingerprint)) {
                report.removedElements.add(e);
            }
        }
        for (DOMElement e : after.elements) {
            if (added.contains(e.fingerprint)) {
                report.addedElements.add(e);
            }
        }

        // 检测修改的元素（id 相同但指纹不同）
        Map<String, DOMElement> beforeById = new HashMap<>();
        for (DOMElement e : before.elements) {
            if (e.id != null && !e.id.isBlank() && !removed.contains(e.fingerprint)) {
                beforeById.put(e.id, e);
            }
        }
        for (DOMElement e : after.elements) {
            if (e.id != null && !e.id.isBlank() && !added.contains(e.fingerprint)) {
                DOMElement old = beforeById.get(e.id);
                if (old != null && !old.fingerprint.equals(e.fingerprint)) {
                    // 这个元素被修改了
                    ChangeReport.ElementChange ec = new ChangeReport.ElementChange();
                    ec.id = e.id;
                    ec.before = old;
                    ec.after = e;
                    ec.changes = detectFieldChanges(old, e);
                    report.modifiedElements.add(ec);

                    // 从 removed/added 中移除（因为它是修改，不是删除+新增）
                    report.removedElements.remove(old);
                    report.addedElements.remove(e);
                }
            }
        }

        report.computeStats();
        return report;
    }

    /**
     * 用 AI 分析变更影响。
     */
    public static ImpactReport analyzeImpact(ChangeReport changeReport) {
        if (!AIClient.isReady()) {
            LogUtils.warn(DOMChangeDetector.class, "AI 未启用，跳过影响分析");
            return buildSimpleImpact(changeReport);
        }

        LogUtils.info(DOMChangeDetector.class, "🤖 AI 正在分析 DOM 变更影响...");

        try {
            StringBuilder msg = new StringBuilder();
            msg.append("## 页面\n");
            msg.append("- 名称: ").append(changeReport.before.pageName).append("\n");
            msg.append("- URL: ").append(changeReport.before.url).append("\n\n");

            msg.append("## 变更统计\n");
            msg.append("- 新增元素: ").append(changeReport.addedCount).append(" 个\n");
            msg.append("- 删除元素: ").append(changeReport.removedCount).append(" 个\n");
            msg.append("- 修改元素: ").append(changeReport.modifiedCount).append(" 个\n\n");

            if (!changeReport.removedElements.isEmpty()) {
                msg.append("## 删除的元素\n");
                for (DOMElement e : changeReport.removedElements) {
                    msg.append("- ").append(e.describe()).append("\n");
                }
                msg.append("\n");
            }

            if (!changeReport.modifiedElements.isEmpty()) {
                msg.append("## 修改的元素\n");
                for (ChangeReport.ElementChange ec : changeReport.modifiedElements) {
                    msg.append("- ID=").append(ec.id)
                       .append(" 变更: ").append(String.join(", ", ec.changes)).append("\n");
                }
                msg.append("\n");
            }

            if (!changeReport.addedElements.isEmpty()) {
                msg.append("## 新增的元素\n");
                int count = 0;
                for (DOMElement e : changeReport.addedElements) {
                    if (count++ >= 10) { msg.append("- ... 还有 " + (changeReport.addedCount - 10) + " 个\n"); break; }
                    msg.append("- ").append(e.describe()).append("\n");
                }
            }

            String result = AIClient.get().chat(AI_IMPACT_PROMPT, msg.toString());
            return parseImpactReport(result, changeReport);
        } catch (Exception e) {
            LogUtils.warn(DOMChangeDetector.class, "AI 影响分析失败: {}", e.getMessage());
            return buildSimpleImpact(changeReport);
        }
    }

    // ========== 内部方法 ==========

    private static void saveSnapshot(DOMSnapshot snapshot, String key) {
        try {
            Files.createDirectories(Paths.get(SNAPSHOT_DIR));
            Path file = Paths.get(SNAPSHOT_DIR, key + ".json");
            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(snapshot);
            Files.writeString(file, json);
        } catch (IOException e) {
            LogUtils.warn(DOMChangeDetector.class, "保存快照失败: {}", e.getMessage());
        }
    }

    private static DOMSnapshot loadSnapshot(String key) {
        try {
            Path file = Paths.get(SNAPSHOT_DIR, key + ".json");
            if (!Files.exists(file)) return null;
            String json = Files.readString(file);
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, DOMSnapshot.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> detectFieldChanges(DOMElement before, DOMElement after) {
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(before.className, after.className)) changes.add("class");
        if (!Objects.equals(before.dataTestId, after.dataTestId)) changes.add("data-testid");
        if (!Objects.equals(before.ariaLabel, after.ariaLabel)) changes.add("aria-label");
        if (!Objects.equals(before.type, after.type)) changes.add("type");
        if (!Objects.equals(before.placeholder, after.placeholder)) changes.add("placeholder");
        if (!Objects.equals(before.role, after.role)) changes.add("role");
        if (!Objects.equals(before.text, after.text)) changes.add("text");
        return changes;
    }

    @SuppressWarnings("unchecked")
    private static ImpactReport parseImpactReport(String json, ChangeReport changeReport) {
        ImpactReport report = new ImpactReport();
        try {
            Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
            report.changeSummary = (String) data.getOrDefault("change_summary", "");
            report.riskLevel = (String) data.getOrDefault("risk_level", "LOW");
            report.affectedPatterns = (List<String>) data.getOrDefault("affected_patterns", List.of());
            report.affectedTestCategories = (List<String>) data.getOrDefault("affected_test_categories", List.of());
            report.safeZones = (List<String>) data.getOrDefault("safe_zones", List.of());
            Object count = data.get("estimated_impact_test_count");
            report.estimatedImpactTestCount = count instanceof Number ? ((Number) count).intValue() : 0;

            List<Map<String, Object>> recs = (List<Map<String, Object>>) data.get("specific_recommendations");
            if (recs != null) {
                for (Map<String, Object> r : recs) {
                    ImpactReport.LocatorRecommendation lr = new ImpactReport.LocatorRecommendation();
                    lr.locator = (String) r.get("locator");
                    lr.reason = (String) r.get("reason");
                    lr.suggestedFix = (String) r.get("suggested_fix");
                    report.recommendations.add(lr);
                }
            }
        } catch (Exception e) {
            report.changeSummary = "解析失败: " + e.getMessage();
        }
        return report;
    }

    private static ImpactReport buildSimpleImpact(ChangeReport changeReport) {
        ImpactReport report = new ImpactReport();
        report.changeSummary = String.format("检测到 %d 个元素新增, %d 个删除, %d 个修改",
                changeReport.addedCount, changeReport.removedCount, changeReport.modifiedCount);
        report.riskLevel = changeReport.modifiedCount > 5 ? "HIGH" :
                           changeReport.modifiedCount > 0 ? "MEDIUM" : "LOW";
        return report;
    }

    private static String getCurrentUrl(WebDriver driver) {
        try { return driver.getCurrentUrl(); } catch (Exception e) { return "unknown"; }
    }

    private static String getTitle(WebDriver driver) {
        try { return driver.getTitle(); } catch (Exception e) { return "unknown"; }
    }

    // ========== 数据模型 ==========

    public static class DOMElement {
        public String tag;
        public String id;
        public String className;
        public String dataTestId;
        public String ariaLabel;
        public String name;
        public String type;
        public String placeholder;
        public String role;
        public String href;
        public String title;
        public String text;
        public String fingerprint;

        /** 计算元素指纹（用于快速对比） */
        void computeFingerprint() {
            StringBuilder sb = new StringBuilder(tag != null ? tag : "?");
            if (id != null) sb.append("#").append(id);
            if (dataTestId != null) sb.append("[data-testid=").append(dataTestId).append("]");
            if (ariaLabel != null) sb.append("[aria=").append(ariaLabel).append("]");
            if (name != null) sb.append("[name=").append(name).append("]");
            if (className != null) sb.append(".").append(className.split(" ")[0]);
            if (text != null && !text.isBlank()) sb.append("{").append(text.substring(0, Math.min(30, text.length()))).append("}");
            this.fingerprint = sb.toString();
        }

        public String describe() {
            StringBuilder sb = new StringBuilder("<" + tag);
            if (id != null) sb.append(" id=\"").append(id).append("\"");
            if (dataTestId != null) sb.append(" data-testid=\"").append(dataTestId).append("\"");
            if (ariaLabel != null) sb.append(" aria-label=\"").append(ariaLabel).append("\"");
            if (text != null && !text.isBlank()) sb.append(">").append(text.substring(0, Math.min(40, text.length())));
            else sb.append(">");
            return sb.toString();
        }
    }

    public static class DOMSnapshot {
        public String pageName;
        public String url;
        public String title;
        public String capturedAt;
        public List<DOMElement> elements = new ArrayList<>();
    }

    public static class ChangeReport {
        public DOMSnapshot before;
        public DOMSnapshot after;
        public final List<DOMElement> removedElements = new ArrayList<>();
        public final List<DOMElement> addedElements = new ArrayList<>();
        public final List<ElementChange> modifiedElements = new ArrayList<>();
        public int addedCount;
        public int removedCount;
        public int modifiedCount;

        void computeStats() {
            addedCount = addedElements.size();
            removedCount = removedElements.size();
            modifiedCount = modifiedElements.size();
        }

        public boolean hasChanges() { return addedCount + removedCount + modifiedCount > 0; }

        public static class ElementChange {
            public String id;
            public DOMElement before;
            public DOMElement after;
            public List<String> changes = List.of();
        }
    }

    public static class ImpactReport {
        public String changeSummary;
        public String riskLevel;
        public List<String> affectedPatterns = List.of();
        public List<String> affectedTestCategories = List.of();
        public List<String> safeZones = List.of();
        public List<LocatorRecommendation> recommendations = new ArrayList<>();
        public int estimatedImpactTestCount;

        public boolean isHighRisk() { return "HIGH".equalsIgnoreCase(riskLevel); }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("═══════════════════════════════════\n");
            sb.append("🔍 DOM 变更影响分析\n");
            sb.append("═══════════════════════════════════\n");
            sb.append("风险等级: ").append(riskLevel).append("\n");
            sb.append("变更摘要: ").append(changeSummary).append("\n");

            if (!affectedPatterns.isEmpty()) {
                sb.append("\n⚠️ 受影响的定位器模式:\n");
                affectedPatterns.forEach(p -> sb.append("  • ").append(p).append("\n"));
            }

            if (!affectedTestCategories.isEmpty()) {
                sb.append("\n📋 受影响的测试类别:\n");
                affectedTestCategories.forEach(c -> sb.append("  • ").append(c).append("\n"));
            }

            if (!safeZones.isEmpty()) {
                sb.append("\n✅ 安全区域:\n");
                safeZones.forEach(z -> sb.append("  • ").append(z).append("\n"));
            }

            if (!recommendations.isEmpty()) {
                sb.append("\n🔧 具体修复建议:\n");
                for (LocatorRecommendation r : recommendations) {
                    sb.append("  • ").append(r.locator).append("\n");
                    sb.append("    原因: ").append(r.reason).append("\n");
                    sb.append("    建议: ").append(r.suggestedFix).append("\n");
                }
            }

            sb.append("\n预计受影响测试数: ").append(estimatedImpactTestCount).append("\n");
            sb.append("═══════════════════════════════════\n");
            return sb.toString();
        }

        public static class LocatorRecommendation {
            public String locator;
            public String reason;
            public String suggestedFix;
        }
    }
}
