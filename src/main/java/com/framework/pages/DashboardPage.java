package com.framework.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import java.util.List;

/**
 * Dashboard 页面（登录后）。
 */
public class DashboardPage extends BasePage {

    private static final By WELCOME_MSG    = By.cssSelector(".welcome-message");
    private static final By USER_AVATAR    = By.cssSelector(".user-avatar");
    private static final By LOGOUT_BUTTON  = By.cssSelector("a[href*='logout']");
    private static final By MENU_ITEMS     = By.cssSelector(".nav-menu a");

    @Override
    public String getPageUrl() { return "/dashboard"; }

    @Override
    public boolean isAt() {
        return actions.isDisplayed(WELCOME_MSG) || actions.isDisplayed(USER_AVATAR);
    }

    @Step("获取欢迎消息")
    public String getWelcomeMessage() {
        return actions.getText(WELCOME_MSG);
    }

    @Step("获取菜单列表")
    public List<String> getNavMenuItems() {
        return actions.getAllTexts(MENU_ITEMS);
    }

    @Step("退出登录")
    public LoginPage logout() {
        actions.click(LOGOUT_BUTTON);
        return new LoginPage();
    }
}
