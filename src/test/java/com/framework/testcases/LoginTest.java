package com.framework.testcases;

import com.framework.pages.DashboardPage;
import com.framework.pages.LoginPage;
import com.framework.framework.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 登录模块测试用例。
 * 继承 BaseTest，通过 actions()/screenshot()/waitStrategy() 方法操作。
 * POM + TestNG + Allure。
 */
public class LoginTest extends BaseTest {

    // ⚠️ 不要在类级别 new PageObject()，driver 此时还不存在
    // 在 @Test 方法内创建，或在测试中用 BasePage.goTo()

    @Test(description = "验证登录页正常加载")
    @Severity(SeverityLevel.BLOCKER)
    public void testLoginPageLoads() {
        LoginPage loginPage = new LoginPage();
        loginPage.navigateTo();
        loginPage.verifyPageLoaded();

        Assert.assertTrue(loginPage.isLoginButtonEnabled(),
                "登录按钮应默认可用");
    }

    @Test(description = "有效凭据登录成功")
    @Severity(SeverityLevel.CRITICAL)
    public void testSuccessfulLogin() {
        LoginPage loginPage = new LoginPage();
        loginPage.navigateTo();
        DashboardPage dashboard = loginPage.login("admin", "password123", DashboardPage.class);

        Assert.assertTrue(dashboard.isAt(),
                "登录成功应跳转 Dashboard");
    }

    @Test(description = "无效凭据登录失败")
    @Severity(SeverityLevel.NORMAL)
    public void testLoginWithInvalidCredentials() {
        LoginPage loginPage = new LoginPage();
        loginPage.navigateTo();
        loginPage.loginExpectingFailure("invalid_user", "wrong_password");

        Assert.assertTrue(loginPage.hasError(),
                "应显示错误提示");
    }

    @Test(description = "空字段登录校验")
    @Severity(SeverityLevel.NORMAL)
    public void testLoginWithEmptyFields() {
        LoginPage loginPage = new LoginPage();
        loginPage.navigateTo();
        loginPage.loginExpectingFailure("", "");

        Assert.assertTrue(loginPage.hasError() || !loginPage.isLoginButtonEnabled(),
                "空字段应触发校验提示");
    }
}
