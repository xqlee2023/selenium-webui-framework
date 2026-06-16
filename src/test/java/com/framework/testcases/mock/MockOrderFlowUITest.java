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
 * 🎭 Mock 下单流程 UI 测试
 * ===========================================
 *
 * Login → ProductList → Detail → Cart → Checkout → OrderConfirmation
 *
 * 运行: {@code mvn test -Dtest=MockOrderFlowUITest}
 */
public class MockOrderFlowUITest extends MockBaseTest {

    /**
     * 场景 1: 完整下单流程
     */
    @Test(description = "完整下单流程：登录→浏览→加购→结算→下单成功")
    @Severity(SeverityLevel.BLOCKER)
    @TestCaseInfo(id = "TC-MOCK-ORDER-001", module = "订单", feature = "下单流程",
                  author = "QingGe", priority = TestCaseInfo.Priority.BLOCKER)
    public void testFullOrderFlow() {
        // Step 1: 登录
        AnnotatedDashboardPage dashboard = new AnnotatedLoginPage(mockDriver)
                .navigateTo().login("admin", "password123");
        Assert.assertTrue(mockDriver.hasPerformed("admin"), "Step1: 登录");

        // Step 2: 浏览商品列表
        AnnotatedProductListPage productList = dashboard.goToProducts();
        Assert.assertTrue(mockDriver.hasPerformed("商品"), "Step2: 商品列表");

        // Step 3: 进入商品详情
        AnnotatedProductDetailPage detail = productList.clickFirstProduct();
        Assert.assertTrue(mockDriver.getCurrentUrl().contains("product"), "Step3: 详情页");

        // Step 4: 加入购物车
        AnnotatedCartPage cart = detail.setQuantity(2).addToCart();
        Assert.assertTrue(mockDriver.hasPerformed("2"), "Step4: 数量=2");

        // Step 5: 去结算
        AnnotatedCheckoutPage checkout = cart.checkout();
        Assert.assertTrue(mockDriver.hasPerformed("结算"), "Step5: 结算");

        // Step 6: 提交订单
        checkout.selectPayment("alipay")
                .applyCoupon("WELCOME2026")
                .addRemark("请尽快发货")
                .submitOrder();

        Assert.assertTrue(mockDriver.hasPerformed("alipay"), "Step6a: 支付宝");
        Assert.assertTrue(mockDriver.hasPerformed("WELCOME2026"), "Step6b: 优惠券");
        Assert.assertTrue(mockDriver.hasPerformed("提交订单"), "Step6c: 提交");
        Assert.assertTrue(mockDriver.getCurrentUrl().contains("order"),
                "Step6d: 下单成功页");
    }

    /**
     * 场景 2: 立即购买（跳过购物车）
     */
    @Test(description = "立即购买——从详情页直接结算")
    @Severity(SeverityLevel.CRITICAL)
    @TestCaseInfo(id = "TC-MOCK-ORDER-002", module = "订单", feature = "快捷购买",
                  author = "QingGe", priority = TestCaseInfo.Priority.CRITICAL)
    public void testBuyNow() {
        new AnnotatedLoginPage(mockDriver).navigateTo()
                .login("admin", "password123");

        new AnnotatedProductListPage(mockDriver).navigateTo()
                .clickFirstProduct()
                .setQuantity(1)
                .buyNow()  // 直接跳过购物车
                .selectPayment("wechat")
                .submitOrder();

        Assert.assertTrue(mockDriver.hasPerformed("立即购买"), "应跳过购物车");
        Assert.assertTrue(mockDriver.hasPerformed("wechat"), "应用了微信支付");
    }

    /**
     * 场景 3: 搜索特定商品后下单
     */
    @Test(description = "搜索商品后下单")
    @Severity(SeverityLevel.NORMAL)
    @TestCaseInfo(id = "TC-MOCK-ORDER-003", module = "订单", feature = "搜索下单",
                  author = "QingGe", priority = TestCaseInfo.Priority.NORMAL)
    public void testSearchAndOrder() {
        new AnnotatedLoginPage(mockDriver).navigateTo()
                .login("admin", "password123");

        new AnnotatedProductListPage(mockDriver).navigateTo()
                .search("iPhone 15")
                .clickFirstProduct()
                .setQuantity(1)
                .addToCart()
                .checkout()
                .selectPayment("card")
                .submitOrder();

        Assert.assertTrue(mockDriver.hasPerformed("iPhone 15"), "应搜索了 iPhone 15");
        Assert.assertTrue(mockDriver.hasPerformed("card"), "应选了银行卡支付");
    }
}
