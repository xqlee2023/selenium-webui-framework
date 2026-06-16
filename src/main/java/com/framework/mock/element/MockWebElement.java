package com.framework.mock.element;

import com.framework.utils.LogUtils;
import org.openqa.selenium.*;

import java.util.List;

/**
 * 🧩 Mock WebElement — 模拟页面元素
 *
 * 所有操作都有日志输出，便于学习执行流程。
 *
 * <h3>链式配置示例</h3>
 * <pre>{@code
 * MockWebElement btn = new MockWebElement(By.id("submit"))
 *     .withText("提交订单")
 *     .disabled();  // 模拟按钮不可点击
 * }</pre>
 *
 * @author Lee
 * @since 3.2.0
 */
public class MockWebElement implements WebElement {

    private final String locator;
    private String text;
    private boolean displayed = true;
    private boolean enabled = true;
    private boolean selected = false;

    public MockWebElement(By by) {
        this.locator = by.toString();
        this.text = "Mock-" + Math.abs(locator.hashCode() % 10000);
    }

    public MockWebElement withText(String text) {
        this.text = text;
        return this;
    }
    public MockWebElement hidden()    { this.displayed = false; return this; }
    public MockWebElement disabled()  { this.enabled = false; return this; }
    public MockWebElement selected()  { this.selected = true; return this; }

    @Override public void click()    { LogUtils.info(getClass(), "🖱️ 点击: {}", locator); }
    @Override public void submit()   { LogUtils.info(getClass(), "📤 提交: {}", locator); }
    @Override public void sendKeys(CharSequence... keys) {
        this.text = String.join("", keys);
        LogUtils.info(getClass(), "⌨️ 输入 '{}' → {}", text, locator);
    }
    @Override public void clear()    { this.text = ""; LogUtils.info(getClass(), "🧹 清空: {}", locator); }

    @Override public String getTagName()       { return guessTag(); }
    @Override public String getAttribute(String n) { return "value".equals(n) ? text : "mock-" + n; }
    @Override public String getDomAttribute(String n) { return getAttribute(n); }
    @Override public String getDomProperty(String n)  { return getAttribute(n); }
    @Override public String getText()          { return text; }
    @Override public String getCssValue(String p) { return "mock-css"; }

    @Override public Point getLocation()     { return new Point(100, 200); }
    @Override public Dimension getSize()     { return new Dimension(300, 40); }
    @Override public Rectangle getRect()     { return new Rectangle(100, 200, 40, 300); }
    @Override public boolean isDisplayed()   { return displayed; }
    @Override public boolean isEnabled()     { return enabled; }
    @Override public boolean isSelected()    { return selected; }

    @Override public WebElement findElement(By by)      { return new MockWebElement(by); }
    @Override public List<WebElement> findElements(By by){ return List.of(new MockWebElement(by)); }
    @Override public <X> X getScreenshotAs(OutputType<X> t) {
        return t.convertFromBase64Png("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==");
    }
    @Override public SearchContext getShadowRoot()      { return this; }

    private String guessTag() {
        String s = locator.toLowerCase();
        if (s.contains("button") || s.contains("type='submit'")) return "button";
        if (s.contains("input") || s.contains("textarea"))       return "input";
        if (s.contains("select"))                                return "select";
        if (s.contains("a ") || s.contains("link"))              return "a";
        if (s.contains("h1")) return "h1";
        return "div";
    }

    @Override public String toString() { return "MockEl[" + locator + "]"; }
}
