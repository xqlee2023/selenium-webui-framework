package com.framework.testcases.mock;

import com.framework.annotations.TestCaseInfo;
import com.framework.mock.base.MockBaseTest;
import com.framework.pages.*;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * ===========================================
 * 🎭 Mock 购物车 UI 测试
 * ===========================================
 *
 * 运行: {@code mvn test -Dtest=MockCartUITest}
 */
public class MockCartUITest extends MockBaseTest {

    @Test(description = "添加商品到购物车")
    @Severity(SeverityLevel.CRITICAL)
    @TestCaseInfo(id = "TC-MOCK-CART-001", module = "购物车", feature = "添加商品",
                  author = "QingGe", priority = TestCaseInfo.Priority.CRITICAL)
    public void testAddToCart() {
        new AnnotatedLoginPage(mockDriver).navigateTo()
                .login("admin", "password123");

        AnnotatedProductListPage productList = new AnnotatedProductListPage(mockDriver)
                .navigateTo();

        productList.addFirstToCart();

        Assert.assertTrue(mockDriver.hasPerformed("add-to-cart"),
                "应执行了加入购物车操作");

        AnnotatedCartPage cart = productList.goToCart();
        Assert.assertTrue(mockDriver.getCurrentUrl().contains("cart"),
                "应进入购物车页面");
    }

    @Test(description = "删除购物车商品")
    @Severity(SeverityLevel.NORMAL)
    @TestCaseInfo(id = "TC-MOCK-CART-002", module = "购物车", feature = "删除商品",
                  author = "QingGe", priority = TestCaseInfo.Priority.NORMAL)
    public void testRemoveFromCart() {
        new AnnotatedLoginPage(mockDriver).navigateTo()
                .login("admin", "password123");

        new AnnotatedProductListPage(mockDriver).navigateTo()
                .clickFirstProduct()
                .setQuantity(2)
                .addToCart();

        new AnnotatedCartPage(mockDriver).navigateTo()
                .removeFirstItem();

        Assert.assertTrue(mockDriver.hasPerformed("btn-remove"),
                "应执行了删除操作");
    }

    @Test(description = "从购物车去结算")
    @Severity(SeverityLevel.CRITICAL)
    @TestCaseInfo(id = "TC-MOCK-CART-003", module = "购物车", feature = "结算",
                  author = "QingGe", priority = TestCaseInfo.Priority.CRITICAL)
    public void testCheckoutFromCart() {
        new AnnotatedLoginPage(mockDriver).navigateTo()
                .login("admin", "password123");

        new AnnotatedProductListPage(mockDriver).navigateTo()
                .clickFirstProduct()
                .setQuantity(1)
                .addToCart();

        AnnotatedCartPage cart = new AnnotatedCartPage(mockDriver).navigateTo();
        Assert.assertTrue(mockDriver.getCurrentUrl().contains("cart"), "在购物车页");

        cart.checkout()
                .selectPayment("alipay")
                .submitOrder();

        Assert.assertTrue(mockDriver.hasPerformed("结算"), "应点击了去结算");
        Assert.assertTrue(mockDriver.hasPerformed("提交订单"), "应提交了订单");
    }

    @Test(description = "使用优惠券下单")
    @Severity(SeverityLevel.NORMAL)
    @TestCaseInfo(id = "TC-MOCK-CART-004", module = "购物车", feature = "优惠券",
                  author = "QingGe", priority = TestCaseInfo.Priority.NORMAL)
    public void testCouponApply() {
        new AnnotatedLoginPage(mockDriver).navigateTo()
                .login("admin", "password123");

        new AnnotatedProductListPage(mockDriver).navigateTo()
                .clickFirstProduct()
                .setQuantity(1)
                .addToCart()
                .checkout()
                .applyCoupon("VIP2026")
                .submitOrder();

        Assert.assertTrue(mockDriver.hasPerformed("VIP2026"), "应使用了 VIP2026 优惠券");
    }
}
