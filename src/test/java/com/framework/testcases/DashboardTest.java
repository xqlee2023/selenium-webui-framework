package com.framework.testcases;

import com.framework.pages.DashboardPage;
import com.framework.pages.LoginPage;
import com.framework.framework.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Example test class: Dashboard tests. */
public class DashboardTest extends BaseTest {

    @Test(description = "Verify dashboard navigation menu")
    @Severity(SeverityLevel.NORMAL)
    @Description("Login and verify dashboard navigation menu items")
    public void testDashboardNavigation() {
        LoginPage loginPage = new LoginPage();
        loginPage.navigateTo();
        DashboardPage dashboard = loginPage.login("admin", "password123", DashboardPage.class);

        var menuItems = dashboard.getNavMenuItems();
        Assert.assertFalse(menuItems.isEmpty(), "Navigation menu should have items");
    }

    @Test(description = "Verify logout works correctly")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Login, then logout and verify redirect to login page")
    public void testLogout() {
        LoginPage loginPage = new LoginPage();
        loginPage.navigateTo();
        DashboardPage dashboard = loginPage.login("admin", "password123", DashboardPage.class);

        var afterLogout = dashboard.logout();
        Assert.assertTrue(loginPage.isAt(), "Should return to login page after logout");
    }
}
