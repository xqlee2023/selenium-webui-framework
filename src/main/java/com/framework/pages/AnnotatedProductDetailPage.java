package com.framework.pages;

import com.framework.annotations.Desc;
import com.framework.browser.DriverManager;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * ===========================================
 * 📦 商品详情页（@FindBy 注解版）
 * ===========================================
 */
public class AnnotatedProductDetailPage extends BasePage {

    @FindBy(css = ".product-detail")
    @Desc("商品详情区域")
    private WebElement productDetail;

    @FindBy(css = ".product-name")
    @Desc("商品名称")
    private WebElement productName;

    @FindBy(css = ".product-price")
    @Desc("商品价格")
    private WebElement productPrice;

    @FindBy(id = "quantity-input")
    @Desc("购买数量")
    private WebElement quantityInput;

    @FindBy(css = ".btn-add-to-cart")
    @Desc("加入购物车按钮")
    private WebElement addToCartBtn;

    @FindBy(css = ".btn-buy-now")
    @Desc("立即购买按钮")
    private WebElement buyNowBtn;

    @FindBy(css = ".product-stock")
    @Desc("库存数量")
    private WebElement stock;

    public AnnotatedProductDetailPage() { super(DriverManager.getDriver()); }
    public AnnotatedProductDetailPage(WebDriver driver) { super(driver); }

    @Override public String getPageUrl() { return "/product/detail"; }
    @Override public boolean isAt() { return productDetail.isDisplayed(); }

    @Step("设置购买数量: {qty}")
    public AnnotatedProductDetailPage setQuantity(int qty) {
        quantityInput.clear();
        quantityInput.sendKeys(String.valueOf(qty));
        return this;
    }

    @Step("加入购物车")
    public AnnotatedCartPage addToCart() {
        addToCartBtn.click();
        return new AnnotatedCartPage(driver);
    }

    @Step("立即购买 → 结算")
    public AnnotatedCheckoutPage buyNow() {
        buyNowBtn.click();
        return new AnnotatedCheckoutPage(driver);
    }

    public String getProductName()  { return productName.getText(); }
    public String getProductPrice() { return productPrice.getText(); }
    public String getStock()        { return stock.getText(); }
}
