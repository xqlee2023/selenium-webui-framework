package com.framework.pages;

import com.framework.utils.LogUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * ===========================================
 * 🔑 登录页面 (LoginPage)
 * ===========================================
 *
 * POM（Page Object Model）示例。
 * 定位器统一在顶部定义，业务流程用方法封装。
 */
public class LoginPage extends BasePage {

    private static final By USERNAME_INPUT   = By.id("username");
    private static final By PASSWORD_INPUT   = By.id("password");
    private static final By LOGIN_BUTTON     = By.cssSelector("button[type='submit']");
    private static final By ERROR_MESSAGE    = By.cssSelector(".error-message");
    private static final By REMEMBER_CHECKBOX = By.cssSelector("input[name='remember']");
    private static final By FORGOT_PASSWORD  = By.linkText("忘记密码？");
    private static final By LOGIN_FORM       = By.id("login-form");

    @Override
    public String getPageUrl() {
        return "/login";
    }

    @Override
    public boolean isAt() {
        return actions.isDisplayed(LOGIN_FORM) || actions.isDisplayed(LOGIN_BUTTON);
    }

    /**
     * 完整登录流程。
     * 输入凭据 → 点击登录 → 等待跳转 → 返回目标页面。
     */
    @Step("登录: username={username}")
    public <T extends BasePage> T login(String username, String password, Class<T> expectedPage) {
        LogUtils.info(getClass(), "🔑 登录: username={}", username);
        enterUsername(username)
                .enterPassword(password)
                .clickLogin();
        wait.waitForPageLoad(driver);
        try {
            return expectedPage.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("创建页面对象失败: " + expectedPage.getSimpleName(), e);
        }
    }

    /** 预期失败的登录（不会等待跳转）。 */
    @Step("尝试登录（预期失败）: {username}")
    public LoginPage loginExpectingFailure(String username, String password) {
        enterUsername(username)
                .enterPassword(password)
                .clickLogin();
        return this;
    }

    @Step("输入用户名: {username}")
    public LoginPage enterUsername(String username) {
        actions.type(USERNAME_INPUT, username);
        return this;
    }

    @Step("输入密码")
    public LoginPage enterPassword(String password) {
        actions.type(PASSWORD_INPUT, password);
        return this;
    }

    @Step("点击登录按钮")
    public LoginPage clickLogin() {
        actions.click(LOGIN_BUTTON);
        return this;
    }

    @Step("点击忘记密码")
    public ForgotPasswordPage clickForgotPassword() {
        actions.click(FORGOT_PASSWORD);
        return new ForgotPasswordPage();
    }

    /** 获取错误提示文字。 */
    public String getErrorMessage() {
        return actions.getText(ERROR_MESSAGE);
    }

    /** 判断是否有错误提示。 */
    public boolean hasError() {
        return actions.isDisplayed(ERROR_MESSAGE);
    }

    /** 登录按钮是否可用。 */
    public boolean isLoginButtonEnabled() {
        return actions.isEnabled(LOGIN_BUTTON);
    }
}
