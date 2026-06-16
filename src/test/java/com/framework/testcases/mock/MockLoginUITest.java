package com.framework.testcases.mock;

import com.framework.annotations.TestCaseInfo;
import com.framework.mock.base.MockBaseTest;
import com.framework.pages.AnnotatedDashboardPage;
import com.framework.pages.AnnotatedLoginPage;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * ===========================================
 * 🎭 Mock 登录 UI 测试
 * ===========================================
 *
 * 使用 @FindBy + @Desc 注解的 Page Object，
 * 在 MockWebDriver 下验证登录流程。
 *
 * <h3>运行</h3>
 * <pre>mvn test -Dtest=MockLoginUITest</pre>
 */
public class MockLoginUITest extends MockBaseTest {

    @Test(description = "有效凭据登录成功")
    @Severity(SeverityLevel.BLOCKER)
    @TestCaseInfo(id = "TC-MOCK-LOGIN-001", module = "登录", feature = "用户认证",
                  author = "QingGe", priority = TestCaseInfo.Priority.BLOCKER)
    @Description("输入正确用户名密码，登录后验证跳转到 Dashboard")
    public void testLoginSuccess() {
        AnnotatedLoginPage loginPage = new AnnotatedLoginPage(mockDriver);

        AnnotatedDashboardPage dashboard = loginPage.navigateTo()
                .login("admin", "password123");

        // Mock 验证：检查关键操作是否执行
        Assert.assertTrue(mockDriver.hasPerformed("admin"),
                "应输入了用户名 'admin'");
        Assert.assertTrue(mockDriver.hasPerformed("password"),
                "应输入了密码");
        Assert.assertTrue(mockDriver.hasPerformed("登录按钮"),
                "应点击了登录按钮");
        Assert.assertTrue(mockDriver.getCurrentUrl().contains("dashboard"),
                "应跳转到 Dashboard 页面");
    }

    @Test(description = "无效凭据登录失败")
    @Severity(SeverityLevel.CRITICAL)
    @TestCaseInfo(id = "TC-MOCK-LOGIN-002", module = "登录", feature = "异常处理",
                  author = "QingGe", priority = TestCaseInfo.Priority.CRITICAL)
    @Description("输入错误密码，应留在登录页")
    public void testLoginInvalidCredentials() {
        new AnnotatedLoginPage(mockDriver)
                .navigateTo()
                .loginExpectingFailure("admin", "wrong_password");

        Assert.assertTrue(mockDriver.hasPerformed("wrong_password"),
                "应尝试了错误密码");
        Assert.assertTrue(mockDriver.getCurrentUrl().contains("login"),
                "应停留在登录页面");
    }

    @Test(description = "空字段校验")
    @Severity(SeverityLevel.NORMAL)
    @TestCaseInfo(id = "TC-MOCK-LOGIN-003", module = "登录", feature = "表单校验",
                  author = "QingGe", priority = TestCaseInfo.Priority.NORMAL)
    @Description("不输入内容直接登录，触发前端校验")
    public void testLoginEmptyFields() {
        new AnnotatedLoginPage(mockDriver)
                .navigateTo()
                .loginExpectingFailure("", "");

        Assert.assertTrue(mockDriver.getCurrentUrl().contains("login"),
                "空字段提交后应停留在登录页");
    }

    @Test(description = "登录页面加载验证")
    @Severity(SeverityLevel.MINOR)
    @TestCaseInfo(id = "TC-MOCK-LOGIN-004", module = "登录", feature = "页面加载",
                  author = "QingGe", priority = TestCaseInfo.Priority.MINOR)
    @Description("验证登录页 URL 和标题")
    public void testLoginPageLoads() {
        new AnnotatedLoginPage(mockDriver).navigateTo();

        Assert.assertEquals(mockDriver.getCurrentUrl(), "https://example.com/login");
        Assert.assertEquals(mockDriver.getTitle(), "用户登录");
    }

    @Test(description = "登录后验证 Dashboard 信息")
    @Severity(SeverityLevel.CRITICAL)
    @TestCaseInfo(id = "TC-MOCK-LOGIN-005", module = "登录", feature = "页面跳转",
                  author = "QingGe", priority = TestCaseInfo.Priority.CRITICAL)
    @Description("登录成功后验证 Dashboard 页面内容")
    public void testLoginAndVerifyDashboard() {
        AnnotatedDashboardPage dashboard = new AnnotatedLoginPage(mockDriver)
                .navigateTo()
                .login("admin", "password123");

        Assert.assertTrue(mockDriver.getCurrentUrl().contains("dashboard"),
                "应跳转到控制台页面");
    }

    @Test(description = "退出登录")
    @Severity(SeverityLevel.NORMAL)
    @TestCaseInfo(id = "TC-MOCK-LOGIN-006", module = "登录", feature = "会话管理",
                  author = "QingGe", priority = TestCaseInfo.Priority.NORMAL)
    @Description("登录后退出，应返回登录页")
    public void testLogout() {
        AnnotatedDashboardPage dashboard = new AnnotatedLoginPage(mockDriver)
                .navigateTo()
                .login("admin", "password123");

        dashboard.logout();

        Assert.assertTrue(mockDriver.hasPerformed("退出"),
                "应执行退出登录操作");
    }
}
