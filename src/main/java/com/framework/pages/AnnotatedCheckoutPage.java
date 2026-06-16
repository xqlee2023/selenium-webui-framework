package com.framework.pages;

import com.framework.annotations.Desc;
import com.framework.browser.DriverManager;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * ===========================================
 * 💳 结算确认页（@FindBy 注解版）
 * ===========================================
 */
public class AnnotatedCheckoutPage extends BasePage {

    @FindBy(id = "address-name")
    @Desc("收货人姓名")
    private WebElement addressName;

    @FindBy(id = "address-phone")
    @Desc("收货人电话")
    private WebElement addressPhone;

    @FindBy(id = "address-detail")
    @Desc("详细地址")
    private WebElement addressDetail;

    @FindBy(css = ".order-summary")
    @Desc("订单摘要")
    private WebElement orderSummary;

    @FindBy(css = ".order-total")
    @Desc("订单总额")
    private WebElement orderTotal;

    @FindBy(id = "payment-alipay")
    @Desc("支付宝支付")
    private WebElement paymentAlipay;

    @FindBy(id = "payment-wechat")
    @Desc("微信支付")
    private WebElement paymentWechat;

    @FindBy(id = "payment-card")
    @Desc("银行卡支付")
    private WebElement paymentCard;

    @FindBy(id = "coupon-input")
    @Desc("优惠券输入框")
    private WebElement couponInput;

    @FindBy(id = "coupon-apply")
    @Desc("使用优惠券按钮")
    private WebElement couponApply;

    @FindBy(id = "remark-input")
    @Desc("订单备注")
    private WebElement remarkInput;

    @FindBy(id = "btn-submit-order")
    @Desc("提交订单按钮")
    private WebElement submitOrderBtn;

    public AnnotatedCheckoutPage() { super(DriverManager.getDriver()); }
    public AnnotatedCheckoutPage(WebDriver driver) { super(driver); }

    @Override public String getPageUrl() { return "/checkout"; }
    @Override public boolean isAt() { return submitOrderBtn.isDisplayed(); }

    @Override
    @Step("进入结算页")
    public AnnotatedCheckoutPage navigateTo() {
        super.navigateTo();
        return this;
    }

    @Step("选择支付方式: {method}")
    public AnnotatedCheckoutPage selectPayment(String method) {
        switch (method.toLowerCase()) {
            case "alipay" -> paymentAlipay.click();
            case "wechat" -> paymentWechat.click();
            case "card"   -> paymentCard.click();
            default -> throw new IllegalArgumentException("未知支付方式: " + method);
        }
        return this;
    }

    @Step("使用优惠券: {code}")
    public AnnotatedCheckoutPage applyCoupon(String code) {
        couponInput.sendKeys(code);
        couponApply.click();
        return this;
    }

    @Step("填写备注: {remark}")
    public AnnotatedCheckoutPage addRemark(String remark) {
        remarkInput.sendKeys(remark);
        return this;
    }

    @Step("提交订单 → 下单成功页")
    public AnnotatedOrderConfirmationPage submitOrder() {
        submitOrderBtn.click();
        return new AnnotatedOrderConfirmationPage(driver);
    }

    public String getTotalPrice()  { return orderTotal.getText(); }
    public String getAddressName() { return addressName.getText(); }
}
