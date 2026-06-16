package com.framework.pages;

import com.framework.annotations.Desc;
import com.framework.browser.DriverManager;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * ===========================================
 * 🛍️ 购物车页（@FindBy 注解版）
 * ===========================================
 */
public class AnnotatedCartPage extends BasePage {

    @FindBy(css = ".cart-item")
    @Desc("购物车商品行列表")
    private java.util.List<WebElement> cartItems;

    @FindBy(css = ".cart-item:nth-child(1) .item-name")
    @Desc("第1个商品名")
    private WebElement firstItemName;

    @FindBy(css = ".cart-item:nth-child(1) .item-price")
    @Desc("第1个商品单价")
    private WebElement firstItemPrice;

    @FindBy(css = ".cart-item:nth-child(1) .item-qty")
    @Desc("第1个商品数量")
    private WebElement firstItemQty;

    @FindBy(css = ".cart-item:nth-child(1) .btn-remove")
    @Desc("第1个商品删除按钮")
    private WebElement firstItemRemove;

    @FindBy(css = ".cart-total")
    @Desc("合计金额")
    private WebElement totalPrice;

    @FindBy(id = "btn-checkout")
    @Desc("去结算按钮")
    private WebElement checkoutBtn;

    @FindBy(css = ".empty-cart")
    @Desc("空购物车提示")
    private WebElement emptyCart;

    @FindBy(linkText = "继续购物")
    @Desc("继续购物链接")
    private WebElement continueShopping;

    public AnnotatedCartPage() { super(DriverManager.getDriver()); }
    public AnnotatedCartPage(WebDriver driver) { super(driver); }

    @Override public String getPageUrl() { return "/cart"; }
    @Override public boolean isAt() { return checkoutBtn.isDisplayed() || emptyCart.isDisplayed(); }

    @Override
    @Step("进入购物车")
    public AnnotatedCartPage navigateTo() {
        super.navigateTo();
        return this;
    }

    @Step("删除第1个商品")
    public AnnotatedCartPage removeFirstItem() {
        firstItemRemove.click();
        return this;
    }

    @Step("去结算 → 结算页")
    public AnnotatedCheckoutPage checkout() {
        checkoutBtn.click();
        return new AnnotatedCheckoutPage(driver);
    }

    @Step("继续购物")
    public AnnotatedProductListPage continueShopping() {
        continueShopping.click();
        return new AnnotatedProductListPage(driver);
    }

    public int getItemCount()         { return cartItems.size(); }
    public String getFirstItemName()  { return firstItemName.getText(); }
    public String getTotalPrice()     { return totalPrice.getText(); }
    public boolean isEmpty()          { return emptyCart.isDisplayed(); }
}
