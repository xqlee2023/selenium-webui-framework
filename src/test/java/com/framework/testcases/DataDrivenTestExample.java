package com.framework.testcases;

import com.framework.builder.TestDataBuilder;
import com.framework.framework.BaseTest;
import io.qameta.allure.Description;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * 数据驱动测试示例。
 * 展示 Builder Pattern + DataProvider 的组合用法。
 */
public class DataDrivenTestExample extends BaseTest {

    @DataProvider(name = "loginUsers")
    public Object[][] loginUsers() {
        return new Object[][]{
                TestDataBuilder.aUser()
                        .withUsername("admin")
                        .withPassword("Admin123!")
                        .with("expected", "success")
                        .buildAsRow(),

                TestDataBuilder.aUser()
                        .withUsername("invalid")
                        .withPassword("wrong")
                        .with("expected", "failure")
                        .buildAsRow(),

                TestDataBuilder.aUser()
                        .withUsername("locked_user")
                        .withPassword("AnyPass1")
                        .locked()
                        .with("expected", "locked")
                        .buildAsRow(),
        };
    }

    @Test(dataProvider = "loginUsers")
    @Description("数据驱动测试：Builder 构建测试数据")
    public void testLoginWithBuilderData(Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");
        String expected = user.get("expected");

        // 这里执行实际的登录测试逻辑
        // 现在只是打印示例
        System.out.printf("Testing login: %s / %s → expected: %s%n",
                username, password, expected);
    }
}
