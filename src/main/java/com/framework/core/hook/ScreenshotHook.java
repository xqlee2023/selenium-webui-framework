package com.framework.core.hook;

import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ISuite;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;
import com.framework.browser.DriverManager;

/**
 * 失败截图 Hook。
 * 单一职责：测试失败时自动截图并附加到 Allure。
 */
public class ScreenshotHook implements TestEventHook {

    @Override
    public void onTestFailure(ITestResult result) {
        if (!DriverManager.hasDriver()) return;
        try {
            byte[] screenshot = ((TakesScreenshot) DriverManager.getDriver())
                    .getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment("失败截图", "image/png",
                    new ByteArrayInputStream(screenshot), "png");
        } catch (Exception e) {
            // 截图失败不阻断测试流程
        }
    }
}
