package com.framework.pages;

import com.framework.annotations.Desc;
import com.framework.browser.DriverManager;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * ===========================================
 * 🛒 商品列表页（@FindBy 注解版）
 * ===========================================
 */
public class AnnotatedProductListPage extends BasePage {

    @FindBy(tagName = "h1")
    @Desc("页面标题")
    private WebElement pageTitle;

    @FindBy(id = "search-input")
    @Desc("搜索框")
    private WebElement searchInput;

    @FindBy(id = "search-btn")
    @Desc("搜索按钮")
    private WebElement searchBtn;

    @FindBy(css = ".product-card")
    @Desc("商品卡片列表")
    private java.util.List<WebElement> productCards;

    @FindBy(css = ".product-card:nth-child(1) .product-name")
    @Desc("第1个商品名称")
    private WebElement firstProductName;

    @FindBy(css = ".product-card:nth-child(1) .product-price")
    @Desc("第1个商品价格")
    private WebElement firstProductPrice;

    @FindBy(css = ".product-card:nth-child(1) .add-to-cart")
    @Desc("第1个商品-加入购物车按钮")
    private WebElement firstAddToCart;

    @FindBy(css = ".product-card:nth-child(1)")
    @Desc("第1个商品卡片")
    private WebElement firstProductCard;

    @FindBy(id = "cart-badge")
    @Desc("购物车数量徽标")
    private WebElement cartBadge;

    @FindBy(linkText = "购物车")
    @Desc("购物车链接")
    private WebElement cartLink;

    public AnnotatedProductListPage() { super(DriverManager.getDriver()); }
    public AnnotatedProductListPage(WebDriver driver) { super(driver); }

    @Override public String getPageUrl() { return "/products"; }
    @Override public boolean isAt() { return pageTitle.isDisplayed(); }

    @Override
    @Step("打开商品列表")
    public AnnotatedProductListPage navigateTo() {
        super.navigateTo();
        return this;
    }

    @Step("搜索: {keyword}")
    public AnnotatedProductListPage search(String keyword) {
        searchInput.sendKeys(keyword);
        searchBtn.click();
        return this;
    }

    @Step("第1个商品加入购物车")
    public AnnotatedProductListPage addFirstToCart() {
        firstAddToCart.click();
        return this;
    }

    @Step("点击第1个商品 → 详情")
    public AnnotatedProductDetailPage clickFirstProduct() {
        firstProductCard.click();
        return new AnnotatedProductDetailPage(driver);
    }

    @Step("进入购物车")
    public AnnotatedCartPage goToCart() {
        cartLink.click();
        return new AnnotatedCartPage(driver);
    }

    public int getProductCount()               { return productCards.size(); }
    public String getFirstProductName()        { return firstProductName.getText(); }
    public String getFirstProductPrice()       { return firstProductPrice.getText(); }
    public String getPageTitle()               { return pageTitle.getText(); }
}
