package com.framework.element.actions;

import com.framework.utils.LogUtils;
import com.framework.wait.WaitStrategyFactory;
import io.qameta.allure.Step;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;

/**
 * ===========================================
 * 🌐 导航操作 (NavigationActions)
 * ===========================================
 *
 * 遵循 单一职责原则：只负责页面级别的导航操作。
 * 从 ElementActions 中拆分出来，独立管理。
 *
 * 包括：打开网址、刷新、前进/后退、窗口管理。
 */
public class NavigationActions {

    private final WebDriver driver;

    public NavigationActions(WebDriver driver) {
        this.driver = driver;
    }

    @Step("导航到 {url}")
    public NavigationActions navigateTo(String url) {
        LogUtils.info(getClass(), "🌐 打开: {}", url);
        driver.get(url);
        WaitStrategyFactory.getStrategy(driver).waitForPageLoad(driver);
        return this;
    }

    @Step("刷新页面")
    public NavigationActions refresh() {
        driver.navigate().refresh();
        WaitStrategyFactory.getStrategy(driver).waitForPageLoad(driver);
        return this;
    }

    @Step("后退")
    public NavigationActions back() {
        driver.navigate().back();
        return this;
    }

    @Step("前进")
    public NavigationActions forward() {
        driver.navigate().forward();
        return this;
    }

    /** 获取当前页面 URL。 */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /** 获取页面标题。 */
    public String getTitle() {
        return driver.getTitle();
    }

    /** 执行 JavaScript。 */
    public Object executeJS(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    /** 切换到新打开的标签页。 */
    @Step("切换到新标签页")
    public NavigationActions switchToNewTab() {
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(handles.get(handles.size() - 1));
        return this;
    }

    /** 切换回第一个标签页。 */
    @Step("切回主标签页")
    public NavigationActions switchToMainTab() {
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(handles.get(0));
        return this;
    }
}
