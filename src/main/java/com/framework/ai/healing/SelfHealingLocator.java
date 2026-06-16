package com.framework.ai.healing;

import com.framework.utils.LogUtils;
import org.openqa.selenium.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.framework.ai.client.AIClient;
import com.framework.ai.client.AIConfig;

/**
 * 定位器自愈引擎。
 *
 * 当元素找不到时，把页面 DOM 结构发给 AI，
 * 让 AI 根据元素的上下文信息（文本、属性、位置）重新生成备选定位器。
 *
 * 流程：原始定位器失败 → 提取 DOM 快照 → AI 推导 → 逐个尝试备选 → 成功则返回元素
 */
public class SelfHealingLocator {

    private static final String SYSTEM_PROMPT = """
            你是一个 Selenium 测试自动化专家，擅长 CSS 和 XPath 选择器。
            你的任务：根据提供的 DOM 结构片段，为一个定位失败的元素推荐备选选择器。
            
            输出 JSON 格式：
            {
              "candidates": [
                {
                  "strategy": "css | xpath | id",
                  "value": "选择器值",
                  "reason": "为什么推荐这个（简短）",
                  "confidence": 0.9
                }
              ]
            }
            
            要求：
            1. 选择器要稳健，优先使用 data-testid、aria-label、id
            2. 避免使用绝对路径、脆弱索引
            3. 至少推荐 1 个，最多 3 个候选
            4. 只输出 JSON，不要其他内容""";

    private final WebDriver driver;
    private final int maxCandidates;
    private final long timeoutMs;

    public SelfHealingLocator(WebDriver driver) {
        this.driver = driver;
        AIConfig cfg = AIClient.getConfig();
        if (cfg == null || !cfg.getSelfHealing().isEnabled()) cfg = null;
        this.maxCandidates = cfg != null ? cfg.getSelfHealing().getMaxCandidates() : 0;
        this.timeoutMs = cfg != null ? cfg.getSelfHealing().getTimeoutMs() : 0;
    }

    /**
     * 尝试自愈定位。如果原始定位器失败，AI 推导备选并逐个尝试。
     *
     * @param originalLocator 原始定位器（已失败）
     * @param pageArea        定位器上下文描述（如 "登录按钮"、"用户名输入框"）
     * @return 找到的元素，如果自愈也失败则抛出原始异常
     */
    public WebElement heal(By originalLocator, String pageArea) {
        if (!AIClient.isReady()) {
            throw new IllegalStateException("AI 未启用，无法自愈");
        }

        long start = System.currentTimeMillis();
        LogUtils.warn(getClass(), "🔧 定位器自愈启动: {} (区域: {})", describeLocator(originalLocator), pageArea);

        try {
            // 1. 获取页面 DOM 快照
            String domSnapshot = captureDOMSnapshot(originalLocator);

            // 2. 让 AI 推导备选定位器
            String userMsg = String.format("""
                    页面区域描述: %s
                    失败的定位器: %s
                    
                    DOM 结构片段:
                    %s
                    """, pageArea, describeLocator(originalLocator), domSnapshot);

            String aiResult = AIClient.get().chat(SYSTEM_PROMPT, userMsg);
            if (aiResult == null) {
                LogUtils.warn(getClass(), "AI 未返回结果，自愈失败");
                throw new NoSuchElementException("定位器自愈失败: " + describeLocator(originalLocator));
            }

            // 3. 解析候选定位器
            List<LocatorCandidate> candidates = parseCandidates(aiResult);
            LogUtils.info(getClass(), "AI 推荐了 {} 个候选定位器", candidates.size());

            // 4. 逐个尝试
            for (LocatorCandidate candidate : candidates) {
                try {
                    By healed = candidate.toBy();
                    WebElement element = driver.findElement(healed);
                    long elapsed = System.currentTimeMillis() - start;

                    LogUtils.info(getClass(), "✅ 自愈成功! {} → {} ({}ms, 置信度: {:.0f}%)",
                            describeLocator(originalLocator), candidate, elapsed, candidate.confidence * 100);

                    // 记录自愈事件，便于后续分析
                    recordHealingEvent(originalLocator, healed, candidate, elapsed);

                    return element;
                } catch (NoSuchElementException e) {
                    LogUtils.warn(getClass(), "候选失败: {}", candidate);
                }
            }

            LogUtils.error(getClass(), "所有 {} 个候选都失败了", candidates.size());
            throw new NoSuchElementException(
                    "定位器自愈失败，原始: " + describeLocator(originalLocator) +
                    ", 尝试了 " + candidates.size() + " 个候选");

        } catch (Exception e) {
            if (e instanceof NoSuchElementException nse) throw nse;
            LogUtils.error(getClass(), "自愈过程异常: {}", e.getMessage());
            throw new NoSuchElementException("定位器自愈异常: " + e.getMessage());
        }
    }

    /**
     * 提取页面 DOM 快照（围绕目标区域的相关元素）。
     */
    private String captureDOMSnapshot(By originalLocator) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // 获取 body 中所有可交互元素的摘要
            Object result = js.executeScript(
                    "var elements = document.body.querySelectorAll('" +
                    "a, button, input, select, textarea, [data-testid], [aria-label], " +
                    "[role=button], [role=link], form, h1, h2, h3, h4, label, span, div');" +
                    "var out = [];" +
                    "var max = 80;" +
                    "for (var i = 0; i < Math.min(elements.length, max); i++) {" +
                    "  var el = elements[i];" +
                    "  var info = {'tag': el.tagName.toLowerCase()};" +
                    "  if (el.id) info.id = el.id;" +
                    "  if (el.className) info.class = el.className.split(' ').slice(0, 3).join(' ');" +
                    "  if (el.getAttribute('data-testid')) info.testid = el.getAttribute('data-testid');" +
                    "  if (el.getAttribute('aria-label')) info.aria = el.getAttribute('aria-label');" +
                    "  if (el.getAttribute('type')) info.type = el.getAttribute('type');" +
                    "  if (el.getAttribute('name')) info.name = el.getAttribute('name');" +
                    "  if (el.getAttribute('placeholder')) info.placeholder = el.getAttribute('placeholder');" +
                    "  var txt = (el.innerText || el.textContent || '').trim().substring(0, 80);" +
                    "  if (txt) info.text = txt;" +
                    "  out.push(info);" +
                    "}" +
                    "return JSON.stringify(out);");
            return result != null ? result.toString() : "{}";
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 解析 AI 返回的候选定位器列表。
     */
    @SuppressWarnings("unchecked")
    private List<LocatorCandidate> parseCandidates(String aiJson) {
        List<LocatorCandidate> list = new ArrayList<>();
        try {
            Map<String, Object> root = new com.fasterxml.jackson.databind.ObjectMapper().readValue(aiJson, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) root.get("candidates");
            if (candidates == null) return list;

            for (Map<String, Object> c : candidates) {
                LocatorCandidate lc = new LocatorCandidate();
                lc.strategy = (String) c.get("strategy");
                lc.value = (String) c.get("value");
                lc.reason = (String) c.get("reason");
                Object conf = c.get("confidence");
                lc.confidence = conf instanceof Number ? ((Number) conf).doubleValue() : 0.5;
                if (lc.strategy != null && lc.value != null) {
                    list.add(lc);
                }
                if (list.size() >= maxCandidates) break;
            }
        } catch (Exception e) {
            LogUtils.warn(getClass(), "解析 AI 候选失败: {}", e.getMessage());
        }
        return list;
    }

    /**
     * 记录自愈事件（方便后续统计和分析）。
     */
    private void recordHealingEvent(By original, By healed, LocatorCandidate candidate, long elapsedMs) {
        // TODO: 可扩展存储到数据库 / Elasticsearch / 文件
        LogUtils.info(getClass(), "[自愈记录] 原始={} → 愈合={} | 置信度={:.0f}% | 耗时={}ms",
                describeLocator(original), describeLocator(healed),
                candidate.confidence * 100, elapsedMs);
    }

    private String describeLocator(By locator) {
        return locator.toString().replace("By.", "").replace(": ", "=");
    }

    // ========== 内部类 ==========

    static class LocatorCandidate {
        String strategy;
        String value;
        String reason;
        double confidence;

        By toBy() {
            return switch (strategy.toLowerCase()) {
                case "css"     -> By.cssSelector(value);
                case "xpath"   -> By.xpath(value);
                case "id"      -> By.id(value);
                case "name"    -> By.name(value);
                case "class"   -> By.className(value);
                case "tagname" -> By.tagName(value);
                default        -> By.cssSelector(value);
            };
        }

        @Override
        public String toString() {
            return strategy + "=" + value + " (" + reason + ")";
        }
    }
}
