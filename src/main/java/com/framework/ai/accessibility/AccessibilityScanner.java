package com.framework.ai.accessibility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.framework.utils.LogUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ===========================================
 * ♿ A11y 无障碍扫描器 (AccessibilityScanner)
 * ===========================================
 *
 * 基于 axe-core 的 Web 无障碍自动检测。
 * 通过 Selenium 注入 axe.min.js → 运行扫描 → 返回违规列表。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * AccessibilityScanner.Result result = AccessibilityScanner.scan(driver);
 * if (result.hasViolations()) {
 *     result.report().forEach(System.out::println);
 * }
 * }</pre>
 *
 * <h3>检测规则（WCAG 2.1）</h3>
 * <ul>
 *   <li>图片是否有 alt 属性</li>
 *   <li>颜色对比度是否足够</li>
 *   <li>表单元素是否有关联 label</li>
 *   <li>是否可用键盘导航</li>
 *   <li>ARIA 属性是否正确</li>
 *   <li>页面是否有标题 & lang 属性</li>
 *   <li>等等 50+ 条规则</li>
 * </ul>
 *
 * @author Lee
 * @since 3.2.0
 */
public final class AccessibilityScanner {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** axe-core CDN (unpkg) */
    private static final String AXE_CDN =
            "https://unpkg.com/axe-core@4.10.3/axe.min.js";

    /** 注入 axe + 运行扫描的 JS 脚本 */
    private static final String AXE_SCAN_SCRIPT = """
            (function() {
                var callback = arguments[arguments.length - 1];
                if (typeof axe === 'undefined') {
                    // 从 CDN 加载 axe-core
                    var script = document.createElement('script');
                    script.src = '%s';
                    script.onload = function() {
                        axe.run(document, {})
                            .then(function(results) { callback(JSON.stringify(results)); })
                            .catch(function(err) { callback(JSON.stringify({error: err.message})); });
                    };
                    script.onerror = function() {
                        callback(JSON.stringify({error: 'axe-core 脚本加载失败（网络不可达）'}));
                    };
                    document.head.appendChild(script);
                } else {
                    axe.run(document, {})
                        .then(function(results) { callback(JSON.stringify(results)); })
                        .catch(function(err) { callback(JSON.stringify({error: err.message})); });
                }
            })();
            """.formatted(AXE_CDN);

    private AccessibilityScanner() {}

    // ══════════ 核心方法 ══════════

    /**
     * 执行无障碍扫描。
     *
     * @param driver 当前 WebDriver
     * @return 扫描结果（无违规时 violations 为空列表）
     */
    public static Result scan(WebDriver driver) {
        if (!(driver instanceof JavascriptExecutor)) {
            LogUtils.warn(AccessibilityScanner.class, "当前 driver 不支持 JS 执行，跳过无障碍扫描");
            return Result.empty();
        }

        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            String json = (String) js.executeAsyncScript(AXE_SCAN_SCRIPT);
            Map<String, Object> raw = JSON.readValue(json,
                    new TypeReference<Map<String, Object>>() {});

            if (raw.containsKey("error")) {
                LogUtils.warn(AccessibilityScanner.class, "axe-core 扫描异常: {}", raw.get("error"));
                return Result.empty();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> violationsRaw =
                    (List<Map<String, Object>>) raw.getOrDefault("violations", List.of());

            List<Violation> violations = violationsRaw.stream()
                    .map(AccessibilityScanner::parseViolation)
                    .collect(Collectors.toList());

            int passes = raw.containsKey("passes")
                    ? ((List<?>) raw.get("passes")).size() : 0;
            int incomplete = raw.containsKey("incomplete")
                    ? ((List<?>) raw.get("incomplete")).size() : 0;

            return new Result(violations, passes, incomplete);

        } catch (Exception e) {
            LogUtils.warn(AccessibilityScanner.class, "无障碍扫描失败: {}", e.getMessage());
            return Result.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static Violation parseViolation(Map<String, Object> raw) {
        String id = (String) raw.getOrDefault("id", "unknown");
        String impact = (String) raw.getOrDefault("impact", "minor");
        String description = (String) raw.getOrDefault("description", "");
        String help = (String) raw.getOrDefault("help", "");
        String helpUrl = (String) raw.getOrDefault("helpUrl", "");

        List<NodeInfo> nodes = new ArrayList<>();
        List<Map<String, Object>> nodesRaw =
                (List<Map<String, Object>>) raw.getOrDefault("nodes", List.of());
        for (Map<String, Object> node : nodesRaw) {
            List<String> targets = new ArrayList<>();
            Object target = node.get("target");
            if (target instanceof List) {
                for (Object t : (List<?>) target) {
                    if (t instanceof List) {
                        targets.add(String.join(" > ", (List<String>) t));
                    } else if (t instanceof String) {
                        targets.add((String) t);
                    }
                }
            }
            String html = (String) node.getOrDefault("html", "");
            String failureSummary = (String) node.getOrDefault("failureSummary", "");
            nodes.add(new NodeInfo(targets, html, failureSummary));
        }

        return new Violation(id, impact, description, help, helpUrl, nodes);
    }

    // ══════════ 结果模型 ══════════

    /** 违规项 */
    public record Violation(
            String id,
            String impact,      // minor / moderate / serious / critical
            String description,
            String help,
            String helpUrl,
            List<NodeInfo> nodes
    ) {}

    /** 违规节点 */
    public record NodeInfo(
            List<String> targets,   // CSS 选择器路径
            String html,            // 元素 HTML 片段
            String failureSummary   // 失败摘要
    ) {}

    /** 扫描结果 */
    public record Result(
            List<Violation> violations,
            int passes,
            int incomplete
    ) {
        public static Result empty() {
            return new Result(List.of(), 0, 0);
        }

        public boolean hasViolations() {
            return violations != null && !violations.isEmpty();
        }

        /** 按严重度分组统计 */
        public Map<String, Long> byImpact() {
            return violations.stream()
                    .collect(Collectors.groupingBy(Violation::impact, Collectors.counting()));
        }

        /** 按严重度过滤 */
        public List<Violation> critical()  { return byImpact("critical"); }
        public List<Violation> serious()   { return byImpact("serious"); }
        public List<Violation> moderate()  { return byImpact("moderate"); }
        public List<Violation> minor()     { return byImpact("minor"); }

        private List<Violation> byImpact(String level) {
            return violations.stream()
                    .filter(v -> level.equals(v.impact))
                    .collect(Collectors.toList());
        }

        /** 生成 Markdown 报告 */
        public String toMarkdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("## ♿ 无障碍检测报告\n\n");

            if (!hasViolations()) {
                sb.append("✅ 未检测到无障碍违规。通过规则: ").append(passes).append("\n");
                return sb.toString();
            }

            var impactCounts = byImpact();
            sb.append("| 严重度 | 数量 |\n|--------|------|\n");
            for (String level : List.of("critical", "serious", "moderate", "minor")) {
                Long count = impactCounts.getOrDefault(level, 0L);
                if (count > 0) {
                    String emoji = switch (level) {
                        case "critical" -> "🔴";
                        case "serious" -> "🟠";
                        case "moderate" -> "🟡";
                        case "minor" -> "🔵";
                        default -> "⚪";
                    };
                    sb.append("| ").append(emoji).append(" ").append(level)
                            .append(" | ").append(count).append(" |\n");
                }
            }

            sb.append("\n### 违规详情\n\n");
            for (Violation v : violations) {
                String emoji = switch (v.impact) {
                    case "critical" -> "🔴";
                    case "serious" -> "🟠";
                    case "moderate" -> "🟡";
                    case "minor" -> "🔵";
                    default -> "⚪";
                };
                sb.append("#### ").append(emoji).append(" ").append(v.description).append("\n");
                sb.append("- **规则:** ").append(v.id).append("\n");
                sb.append("- **修复指引:** [").append(v.help).append("](").append(v.helpUrl).append(")\n");
                sb.append("- **受影响元素:**\n");
                for (NodeInfo node : v.nodes) {
                    sb.append("  - `").append(String.join(", ", node.targets)).append("`\n");
                    sb.append("    > ").append(node.html).append("\n");
                }
                sb.append("\n");
            }

            return sb.toString();
        }
    }
}
