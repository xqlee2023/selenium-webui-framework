package com.framework.pages;

import com.framework.annotations.Desc;
import com.framework.browser.DriverManager;
import com.framework.utils.LogUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * ===========================================
 * 🔑 登录页面（@FindBy 注解版）
 * ===========================================
 *
 * 对比旧版 LoginPage（By 常量模式）：
 *   旧: private static final By USERNAME = By.id("username");
 *       actions.type(USERNAME, "admin");
 *   新: @FindBy(id = "username") @Desc("用户名输入框")
 *       private WebElement usernameInput;
 *       actions.type(usernameInput, "admin");
 *
 * 优势：
 *   1. @FindBy 是 Selenium 标准，所有自动化工程师都认识
 *   2. @Desc 提供业务语义，可被日志/报告提取
 *   3. PageFactory.initElements() 自动注入，无需手动 set
 *   4. WebElement 字段可直接传参，比 By + driver.findElement 少一步
 */
public class AnnotatedLoginPage extends BasePage {

    // ══════════ 元素定位（@FindBy + @Desc） ══════════

    @FindBy(id = "username")
    @Desc("用户名输入框")
    private WebElement usernameInput;

    @FindBy(id = "password")
    @Desc("密码输入框")
    private WebElement passwordInput;

    @FindBy(css = "button[type='submit']")
    @Desc("登录按钮")
    private WebElement loginButton;

    @FindBy(css = ".error-message")
    @Desc("错误提示信息")
    private WebElement errorMessage;

    @FindBy(css = "input[name='remember']")
    @Desc("记住我复选框")
    private WebElement rememberMeCheckbox;

    @FindBy(linkText = "忘记密码？")
    @Desc("忘记密码链接")
    private WebElement forgotPasswordLink;

    @FindBy(id = "login-form")
    @Desc("登录表单")
    private WebElement loginForm;

    // ══════════ 构造函数 ══════════

    public AnnotatedLoginPage() {
        super(DriverManager.getDriver());
    }

    public AnnotatedLoginPage(WebDriver driver) {
        super(driver);
    }

    // ══════════ 页面信息 ══════════

    @Override
    public String getPageUrl() { return "/login"; }

    @Override
    public boolean isAt() {
        return loginForm.isDisplayed() || loginButton.isDisplayed();
    }

    // ══════════ 业务操作 ══════════

    @Step("打开登录页面")
    @Override
    public AnnotatedLoginPage navigateTo() {
        super.navigateTo();
        return this;
    }

    /**
     * 正常登录流程 → 跳转到 Dashboard
     */
    @Step("登录: user={username}")
    public AnnotatedDashboardPage login(String username, String password) {
        LogUtils.info(getClass(), "🔑 开始登录: {}", username);
        enterUsername(username)
                .enterPassword(password)
                .clickLogin();
        wait.waitForPageLoad(driver);
        return new AnnotatedDashboardPage(driver);
    }

    /**
     * 预期失败的登录（留在当前页）
     */
    @Step("尝试登录（预期失败）: {username}")
    public AnnotatedLoginPage loginExpectingFailure(String username, String password) {
        enterUsername(username)
                .enterPassword(password)
                .clickLogin();
        return this;
    }

    @Step("输入用户名: {username}")
    public AnnotatedLoginPage enterUsername(String username) {
        usernameInput.clear();
        usernameInput.sendKeys(username);
        return this;
    }

    @Step("输入密码: ***")
    public AnnotatedLoginPage enterPassword(String password) {
        passwordInput.clear();
        passwordInput.sendKeys(password);
        return this;
    }

    @Step("点击登录按钮")
    public AnnotatedLoginPage clickLogin() {
        loginButton.click();
        return this;
    }

    @Step("点击忘记密码")
    public void clickForgotPassword() {
        forgotPasswordLink.click();
    }

    // ══════════ 状态查询 ══════════

    public String getErrorMessage() {
        return errorMessage.getText();
    }

    public boolean hasError() {
        return errorMessage.isDisplayed();
    }

    public boolean isLoginButtonEnabled() {
        return loginButton.isEnabled();
    }
}
