package com.framework.pages;

import com.framework.annotations.Desc;
import com.framework.browser.DriverManager;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * ===========================================
 * ✅ 下单成功页（@FindBy 注解版）
 * ===========================================
 */
public class AnnotatedOrderConfirmationPage extends BasePage {

    @FindBy(css = ".success-icon")
    @Desc("成功图标")
    private WebElement successIcon;

    @FindBy(css = ".success-message")
    @Desc("成功提示")
    private WebElement successMessage;

    @FindBy(css = ".order-number")
    @Desc("订单编号")
    private WebElement orderNumber;

    @FindBy(css = ".order-amount")
    @Desc("订单金额")
    private WebElement orderAmount;

    @FindBy(linkText = "查看订单详情")
    @Desc("查看订单详情链接")
    private WebElement viewOrderLink;

    @FindBy(linkText = "返回首页")
    @Desc("返回首页链接")
    private WebElement backHomeLink;

    public AnnotatedOrderConfirmationPage() { super(DriverManager.getDriver()); }
    public AnnotatedOrderConfirmationPage(WebDriver driver) { super(driver); }

    @Override public String getPageUrl() { return "/order/success"; }
    @Override public boolean isAt() { return successIcon.isDisplayed(); }

    public String getOrderNumber()     { return orderNumber.getText(); }
    public String getOrderAmount()     { return orderAmount.getText(); }
    public boolean isSuccess()         { return successIcon.isDisplayed(); }
    public String getSuccessMessage()  { return successMessage.getText(); }

    @Step("查看订单详情")
    public void viewOrderDetail() { viewOrderLink.click(); }

    @Step("返回首页")
    public AnnotatedDashboardPage backToHome() {
        backHomeLink.click();
        return new AnnotatedDashboardPage(driver);
    }
}
