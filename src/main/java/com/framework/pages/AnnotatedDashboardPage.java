package com.framework.pages;

import com.framework.annotations.Desc;
import com.framework.browser.DriverManager;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * ===========================================
 * 📊 Dashboard 页（@FindBy 注解版）
 * ===========================================
 */
public class AnnotatedDashboardPage extends BasePage {

    @FindBy(tagName = "h1")
    @Desc("页面标题")
    private WebElement pageTitle;

    @FindBy(className = "welcome-message")
    @Desc("欢迎语")
    private WebElement welcomeMessage;

    @FindBy(className = "user-avatar")
    @Desc("用户头像")
    private WebElement userAvatar;

    @FindBy(id = "user-dropdown")
    @Desc("用户下拉菜单")
    private WebElement userDropdownMenu;

    @FindBy(linkText = "退出登录")
    @Desc("退出登录链接")
    private WebElement logoutLink;

    @FindBy(linkText = "商品列表")
    @Desc("商品列表入口")
    private WebElement productListLink;

    @FindBy(linkText = "我的订单")
    @Desc("我的订单入口")
    private WebElement myOrdersLink;

    @FindBy(id = "cart-icon")
    @Desc("购物车图标")
    private WebElement cartIcon;

    public AnnotatedDashboardPage() { super(DriverManager.getDriver()); }
    public AnnotatedDashboardPage(WebDriver driver) { super(driver); }

    @Override public String getPageUrl() { return "/dashboard"; }
    @Override public boolean isAt() { return welcomeMessage.isDisplayed() || userAvatar.isDisplayed(); }

    @Override
    @Step("打开控制台")
    public AnnotatedDashboardPage navigateTo() {
        super.navigateTo();
        return this;
    }

    @Step("跳转商品列表")
    public AnnotatedProductListPage goToProducts() {
        productListLink.click();
        return new AnnotatedProductListPage(driver);
    }

    @Step("跳转购物车")
    public AnnotatedCartPage goToCart() {
        cartIcon.click();
        return new AnnotatedCartPage(driver);
    }

    @Step("退出登录")
    public AnnotatedLoginPage logout() {
        userDropdownMenu.click();
        logoutLink.click();
        return new AnnotatedLoginPage(driver);
    }

    public String getWelcomeMessage() { return welcomeMessage.getText(); }
    public String getPageTitle()      { return pageTitle.getText(); }
    public boolean isLoggedIn()       { return userAvatar.isDisplayed(); }
}
