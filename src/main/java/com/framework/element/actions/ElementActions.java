package com.framework.element.actions;

import com.framework.core.ConfigManager;
import com.framework.wait.WaitStrategy;
import com.framework.wait.WaitStrategyFactory;
import com.framework.element.healing.ElementHealingStrategy;
import com.framework.element.healing.HealeniumHealingStrategy;
import com.framework.element.healing.AIHealingStrategy;
import com.framework.element.healing.CompositeHealingStrategy;
import com.framework.utils.LogUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;
import com.framework.element.factory.ElementFactory;

/**
 * ===========================================
 * 🖱️ 元素操作 (ElementActions)
 * ===========================================
 *
 * 遵循 单一职责原则：只负责元素层面的操作（点击、输入、选择）。
 * 不负责页面导航、弹窗、iframe 切换等高层操作。
 * 自愈逻辑通过 ElementHealingStrategy 注入，可插拔。
 *
 * 每个操作：先等待 → 高亮 → 日志 → 执行 → 返回 this（链式）
 *
 * 使用方式：
 *   ElementActions.with(driver)
 *       .type(By.id("username"), "admin")
 *       .click(By.id("login-btn"));
 */
public class ElementActions {

    private final WebDriver driver;
    private final WaitStrategy wait;
    private final ElementHealingStrategy healing;

    public ElementActions(WebDriver driver) {
        this(driver, resolveHealingStrategy());
    }

    /** 完整构造：注入 driver + 自愈策略。 */
    public ElementActions(WebDriver driver, ElementHealingStrategy healing) {
        this.driver = driver;
        this.wait = WaitStrategyFactory.getStrategy(driver);
        this.healing = healing;
    }

    /** 静态工厂方法 —— 更简洁的创建方式 */
    public static ElementActions with(WebDriver driver) {
        return new ElementActions(driver);
    }

    /** 获取等待策略（方便直接调 wait 方法） */
    public WaitStrategy waitStrategy() { return wait; }

    // ========== 🤖 自愈方法 ==========

    /**
     * 带自愈的点击。原始定位器失败时，通过注入的自愈策略重试。
     *
     * @param locator  原始定位器
     * @param pageArea 元素描述（如 "登录按钮"），帮助 AI 理解上下文
     */
    @Step("点击 {locator} (自愈)")
    public ElementActions clickWithHeal(By locator, String pageArea) {
        try {
            return click(locator);
        } catch (NoSuchElementException | TimeoutException e) {
            if (healing == null) throw e;
            try {
                WebElement healed = healing.heal(locator, pageArea, driver);
                highlightElement(healed);
                LogUtils.info(getClass(), "🖱️ 自愈点击 {}", describeElement(healed));
                healed.click();
                return this;
            } catch (Exception healEx) {
                LogUtils.error(getClass(), "点击自愈也失败: {}", healEx.getMessage());
            }
            throw e;
        }
    }

    /**
     * 带自愈的输入。
     */
    @Step("在 {locator} 输入 '{value}' (自愈)")
    public ElementActions typeWithHeal(By locator, String value, String pageArea) {
        try {
            return type(locator, value);
        } catch (NoSuchElementException | TimeoutException e) {
            if (healing == null) throw e;
            try {
                WebElement healed = healing.heal(locator, pageArea, driver);
                highlightElement(healed);
                healed.clear();
                healed.sendKeys(value);
                return this;
            } catch (Exception healEx) {
                LogUtils.error(getClass(), "输入自愈也失败: {}", healEx.getMessage());
            }
            throw e;
        }
    }

    /**
     * 带自愈的元素获取。
     */
    public WebElement findElementWithHeal(By locator, String pageArea) {
        try {
            return wait.waitForPresence(locator);
        } catch (NoSuchElementException | TimeoutException e) {
            if (healing == null) throw e;
            try {
                return healing.heal(locator, pageArea, driver);
            } catch (Exception healEx) {
                LogUtils.error(getClass(), "元素自愈也失败: {}", healEx.getMessage());
            }
            throw e;
        }
    }

    // ========== 点击 ==========

    @Step("点击 {locator}")
    public ElementActions click(By locator) {
        WebElement element = wait.waitForClickable(locator);
        highlightElement(element);
        LogUtils.info(getClass(), "🖱️ 点击 {}", locator);
        element.click();
        return this;
    }

    @Step("JS点击 {locator}")
    public ElementActions clickJS(By locator) {
        WebElement element = wait.waitForPresence(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        return this;
    }

    /** 如果元素存在且可点击则点击，否则忽略 */
    public ElementActions clickIfPresent(By locator) {
        try {
            WebElement el = wait.waitForClickable(locator);
            el.click();
        } catch (TimeoutException e) {
            LogUtils.warn(getClass(), "元素不可点击，跳过: {}", locator);
        }
        return this;
    }

    // ========== 输入 ==========

    @Step("在 {locator} 输入 '{value}'")
    public ElementActions type(By locator, String value) {
        WebElement element = wait.waitForVisible(locator);
        highlightElement(element);
        element.clear();
        element.sendKeys(value);
        return this;
    }

    @Step("在 {locator} 追加 '{value}'")
    public ElementActions append(By locator, String value) {
        WebElement element = wait.waitForVisible(locator);
        element.sendKeys(value);
        return this;
    }

    @Step("清空 {locator}")
    public ElementActions clear(By locator) {
        wait.waitForVisible(locator).clear();
        return this;
    }

    @Step("按 Enter")
    public ElementActions pressEnter(By locator) {
        wait.waitForVisible(locator).sendKeys(Keys.ENTER);
        return this;
    }

    // ========== 选择 ==========

    @Step("选择值 '{value}' 从 {locator}")
    public ElementActions selectByValue(By locator, String value) {
        new Select(wait.waitForVisible(locator)).selectByValue(value);
        return this;
    }

    @Step("选择文字 '{text}' 从 {locator}")
    public ElementActions selectByText(By locator, String text) {
        new Select(wait.waitForVisible(locator)).selectByVisibleText(text);
        return this;
    }

    // ========== 获取信息 ==========

    public String getText(By locator) {
        return wait.waitForVisible(locator).getText();
    }

    public String getAttribute(By locator, String attribute) {
        return wait.waitForPresence(locator).getAttribute(attribute);
    }

    public List<String> getAllTexts(By locator) {
        return ElementFactory.findAll(driver, locator).stream()
                .map(WebElement::getText)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public int count(By locator) {
        return ElementFactory.findAll(driver, locator).size();
    }

    // ========== 状态判断 ==========

    public boolean isDisplayed(By locator) {
        try {
            return wait.waitForVisible(locator).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    public boolean isEnabled(By locator) {
        try {
            return wait.waitForPresence(locator).isEnabled();
        } catch (TimeoutException e) {
            return false;
        }
    }

    public boolean isSelected(By locator) {
        try {
            return wait.waitForPresence(locator).isSelected();
        } catch (TimeoutException e) {
            return false;
        }
    }

    // ========== 高级操作 ==========

    @Step("滚动到 {locator}")
    public ElementActions scrollTo(By locator) {
        WebElement element = wait.waitForPresence(locator);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", element);
        return this;
    }

    @Step("高亮元素 {locator}")
    public ElementActions highlight(By locator) {
        try {
            WebElement el = driver.findElement(locator);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.border='3px solid red'; arguments[0].style.outline='2px solid yellow';",
                    el);
        } catch (Exception ignored) {}
        return this;
    }

    /** 获取当前页面的基础 DOM 信息（用于失败诊断） */
    public String getPageSnapshot() {
        try {
            return (String) ((JavascriptExecutor) driver).executeScript(
                    "return document.title + ' | ' + document.URL + ' | body: ' + " +
                    "(document.body ? document.body.innerText.substring(0, 200) : 'no body')");
        } catch (Exception e) {
            return "Page snapshot unavailable: " + e.getMessage();
        }
    }

    // ========== 内部方法 ==========

    private void highlightElement(WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.border='3px solid red'", element);
        } catch (Exception ignored) {}
    }

    /** 生成元素描述（用于日志和 AI 上下文） */
    private String describeElement(WebElement element) {
        try {
            String tag = element.getTagName();
            String id = element.getAttribute("id");
            String dataTestId = element.getAttribute("data-testid");
            String text = element.getText();
            if (dataTestId != null && !dataTestId.isBlank()) return tag + "[data-testid=" + dataTestId + "]";
            if (id != null && !id.isBlank()) return tag + "#" + id;
            if (text != null && !text.isBlank()) return tag + "(\"" + text.substring(0, Math.min(30, text.length())) + "\")";
            return tag;
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** 根据配置解析自愈策略（组合模式：Healenium 优先 → AI 兜底）。 */
    private static ElementHealingStrategy resolveHealingStrategy() {
        ConfigManager cfg = ConfigManager.get();
        if (!cfg.aiEnabled() || !cfg.aiConfig().getSelfHealing().isEnabled()) {
            // 自愈总开关关闭 → 但 Healenium 仍然可用（无需 AI）
            return HealeniumHealingStrategy.isAvailable()
                    ? new HealeniumHealingStrategy()
                    : null;
        }

        // 组合策略：Healenium（快）→ AI（准）
        if (HealeniumHealingStrategy.isAvailable()) {
            return new CompositeHealingStrategy(
                    new HealeniumHealingStrategy(),
                    new AIHealingStrategy()
            );
        }

        // Healenium 不可用 → 仅 AI
        return new AIHealingStrategy();
    }
}
