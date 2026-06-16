package com.framework.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * 忘记密码页面。
 */
public class ForgotPasswordPage extends BasePage {

    private static final By EMAIL_INPUT  = By.id("email");
    private static final By RESET_BUTTON = By.cssSelector("button[type='submit']");
    private static final By SUCCESS_MSG  = By.cssSelector(".success-message");

    @Override
    public String getPageUrl() { return "/forgot-password"; }

    @Override
    public boolean isAt() {
        return actions.isDisplayed(EMAIL_INPUT);
    }

    @Step("重置密码: {email}")
    public ForgotPasswordPage requestReset(String email) {
        actions.type(EMAIL_INPUT, email).click(RESET_BUTTON);
        return this;
    }

    public boolean isSuccessMessageDisplayed() {
        return actions.isDisplayed(SUCCESS_MSG);
    }
}
