package com.framework.mock.driver;

import com.framework.mock.element.MockWebElement;
import com.framework.utils.LogUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.logging.Logs;
import org.openqa.selenium.interactions.*;

import java.time.Duration;
import java.util.*;
import java.net.URL;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ===========================================
 * 🎭 Mock WebDriver — 零依赖学习模式
 * ===========================================
 *
 * 完全模拟浏览器行为，无需安装 Chrome/Firefox/驱动。
 * 所有操作（打开页面、查找元素、点击、输入）都会被记录到操作历史中。
 *
 * <h3>设计目的</h3>
 * <ul>
 *   <li>学习 Page Object 模式时不用折腾环境</li>
 *   <li>所有操作都有日志，方便理解执行流程</li>
 *   <li>用 {@code getActionHistory()} 验证操作是否按预期执行</li>
 * </ul>
 *
 * <h3>如何切换到真实浏览器？</h3>
 * <pre>
 * // Mock:
 * MockWebDriver driver = new MockWebDriver();
 *
 * // 真实 Chrome（改一行即可）:
 * ChromeDriver driver = new ChromeDriver();
 * </pre>
 *
 * <h3>内建页面</h3>
 * <pre>
 * /login        → 登录页
 * /dashboard    → 控制台
 * /products     → 商品列表
 * /product/:id  → 商品详情
 * /cart         → 购物车
 * /checkout     → 结算确认
 * /order/success → 下单成功
 * /orders       → 我的订单
 * </pre>
 *
 * @author Lee
 * @since 3.2.0
 */
public class MockWebDriver implements WebDriver, JavascriptExecutor, TakesScreenshot {

    private String currentUrl = "about:blank";
    private String currentTitle = "Mock Browser";

    /** 注册的页面映射（线程安全懒初始化，读取 ConfigManager.baseUrl） */
    private static volatile Map<String, String> pages = null;

    private static Map<String, String> getPages() {
        Map<String, String> result = pages;
        if (result != null) return result;
        synchronized (MockWebDriver.class) {
            if (pages != null) return pages;
            pages = doBuildPages();
        }
        return pages;
    }

    private static Map<String, String> doBuildPages() {
        Map<String, String> map = new LinkedHashMap<>();
        String base = "https://example.com";
        try {
            String configured = com.framework.core.ConfigManager.get().baseUrl();
            if (configured != null && !configured.isBlank()) base = configured;
        } catch (Exception e) {
            // ConfigManager 未初始化时使用默认值
        }
        map.put(base + "/login",            "用户登录");
        map.put(base + "/dashboard",         "控制台");
        map.put(base + "/products",          "商品列表");
        map.put(base + "/product/1",         "商品详情 - iPhone 15");
        map.put(base + "/product/2",         "商品详情 - MacBook Pro");
        map.put(base + "/product/3",         "商品详情 - AirPods Pro");
        map.put(base + "/cart",              "购物车");
        map.put(base + "/checkout",          "结算确认");
        map.put(base + "/order/success",     "下单成功");
        map.put(base + "/orders",            "我的订单");
        return map;
    }

    /** 操作历史 */
    private final List<String> actions = new CopyOnWriteArrayList<>();

    // ══════════ 核心操作 ══════════

    @Override
    public void get(String url) {
        this.currentUrl = url;
        this.currentTitle = getPages().getOrDefault(url, "Mock Page");
        record("📄 打开页面: %s → 标题: %s".formatted(url, currentTitle));
    }

    @Override
    public String getCurrentUrl()   { return currentUrl; }
    @Override
    public String getTitle()        { return currentTitle; }
    @Override
    public String getPageSource()   { return "<html><body><h1>" + currentTitle + "</h1></body></html>"; }

    // ══════════ 元素查找 ══════════

    @Override
    public WebElement findElement(By by) {
        MockWebElement el = new MockWebElement(by);
        record("🔍 查找元素: " + by);
        return el;
    }

    @Override
    public List<WebElement> findElements(By by) {
        record("🔍 查找元素列表: " + by);
        return List.of(new MockWebElement(by), new MockWebElement(by), new MockWebElement(by));
    }

    // ══════════ 导航 ══════════

    @Override public Navigation navigate() { return new MockNav(); }
    @Override public void close()           { record("❌ 关闭窗口"); }
    @Override public void quit()            { record("🛑 退出浏览器"); }
    @Override public Set<String> getWindowHandles() { return Set.of("w1"); }
    @Override public String getWindowHandle()       { return "w1"; }
    @Override public TargetLocator switchTo()       { return new MockTarget(); }
        public Window window()                { return new MockWin(); }
    @Override public Options manage()               { return new MockOpts(); }

    // ══════════ 截图 ══════════

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) {
        record("📸 截图（Mock）");
        return target.convertFromBase64Png("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==");
    }

    // ══════════ JS ══════════

    @Override
    public Object executeScript(String script, Object... args) {
        record("⚡ JS: " + script.substring(0, Math.min(script.length(), 50)) + "...");
        return null;
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        record("⚡ Async JS...");
        return null;
    }

        public void manageCookies() {} // 走 manage().getCookies()

    // ══════════ 操作记录 ══════════

    private void record(String action) {
        actions.add(action);
        LogUtils.info(getClass(), "[Mock] {}", action);
    }

    /** 获取全部操作历史 */
    public List<String> getActionHistory() {
        return new ArrayList<>(actions);
    }

    /** 是否执行过包含某关键词的操作 */
    public boolean hasPerformed(String keyword) {
        return actions.stream().anyMatch(a -> a.contains(keyword));
    }

    /** 清空历史 */
    public void clearHistory() { actions.clear(); }

    /** 打印操作时间线 */
    public void printTimeline() {
        LogUtils.info(getClass(), "══════ 操作时间线 ({}) ══════", actions.size());
        for (int i = 0; i < actions.size(); i++) {
            LogUtils.info(getClass(), "  [{}/{}] {}", i + 1, actions.size(), actions.get(i));
        }
        LogUtils.info(getClass(), "═════════════════════════════");
    }

    // ══════════ 内部类 ══════════

    private class MockNav implements Navigation {
        @Override public void back()      { record("⬅️ 后退"); }
        @Override public void forward()   { record("➡️ 前进"); }
        @Override public void to(String url)  { get(url); }
        @Override public void to(URL url)     { get(url.toString()); }
        @Override public void refresh()       { record("🔄 刷新"); }
    }

    private class MockWin implements Window {
        @Override public Dimension getSize()    { return new Dimension(1920, 1080); }
        @Override public void setSize(Dimension d) { record("📐 窗口: " + d); }
        @Override public Point getPosition()    { return new Point(0, 0); }
        @Override public void setPosition(Point p) {}
        @Override public void maximize()   { record("📐 最大化"); }
        @Override public void minimize()   { record("📐 最小化"); }
        @Override public void fullscreen() { record("📐 全屏"); }
    }

    private class MockTarget implements TargetLocator {
        @Override public WebDriver frame(int i)     { record("🖼️ iframe[%d]".formatted(i)); return this.getOuter(); }
        @Override public WebDriver frame(String s)  { record("🖼️ iframe: %s".formatted(s)); return this.getOuter(); }
        @Override public WebDriver frame(WebElement e) { record("🖼️ iframe element"); return this.getOuter(); }
        @Override public WebDriver parentFrame()    { record("🖼️ parent frame"); return this.getOuter(); }
        @Override public WebDriver defaultContent() { record("🖼️ default content"); return this.getOuter(); }
        @Override public WebElement activeElement() { return findElement(By.cssSelector(":focus")); }
        @Override public Alert alert() {
            record("🚨 Alert"); return new Alert() {
                @Override public void dismiss() { record("🚨 Alert → 取消"); }
                @Override public void accept()  { record("🚨 Alert → 确定"); }
                @Override public String getText() { return "Mock Alert"; }
                @Override public void sendKeys(String s) { record("🚨 Alert 输入: " + s); }
            };
        }
        @Override public WebDriver newWindow(WindowType t) { record("🪟 new " + t); return this.getOuter(); }
        @Override public WebDriver window(String s) { return this.getOuter(); }
        private MockWebDriver getOuter() { return MockWebDriver.this; }
    }

    private class MockOpts implements Options {
        @Override public void addCookie(Cookie c)           { record("🍪 +Cookie: " + c.getName()); }
        @Override public void deleteCookieNamed(String n)    { record("🍪 -Cookie: " + n); }
        @Override public void deleteCookie(Cookie c)         { record("🍪 -Cookie: " + c.getName()); }
        @Override public void deleteAllCookies()             { record("🍪 清除所有 Cookie"); }
        @Override public Set<Cookie> getCookies()            { return Set.of(new Cookie("s", "mock")); }
        @Override public Cookie getCookieNamed(String n)     { return new Cookie(n, "mock"); }
        @Override public Timeouts timeouts() {
            return new Timeouts() {
                @Override public Timeouts implicitlyWait(Duration d) { record("⏱️ 隐式等待: " + d); return this; }
                @Override public Timeouts implicitlyWait(long time, java.util.concurrent.TimeUnit unit) { record("⏱️ 隐式等待: " + time + " " + unit); return this; }
                @Override public Timeouts pageLoadTimeout(Duration d) { record("⏱️ 页面超时: " + d); return this; }
                @Override public Timeouts pageLoadTimeout(long time, java.util.concurrent.TimeUnit unit) { record("⏱️ 页面超时: " + time + " " + unit); return this; }
                @Override public Timeouts setScriptTimeout(long time, java.util.concurrent.TimeUnit unit) { record("⏱️ 页面超时: " + time + " " + unit); return this; }
                @Override public Timeouts scriptTimeout(Duration d)   { record("⏱️ 脚本超时: " + d); return this; }
            };
        }
            public Window window()                { return new MockWin(); }
        @Override public Logs logs()      { return null; }
    }
}
