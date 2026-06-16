package com.framework.framework;

import com.framework.utils.DateUtils;
import com.framework.utils.LogUtils;
import io.qameta.allure.Attachment;
import org.openqa.selenium.*;
import org.openqa.selenium.io.FileHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.framework.core.ConfigManager;

/**
 * ===========================================
 * 📸 截图工具类 (ScreenshotHelper)
 * ===========================================
 *
 * 提供三种截图方式：
 *   - 视口截图：只截当前屏幕看到的部分
 *   - 全页截图：滚动截取整个页面（包含折叠起来的部分）
 *   - 元素截图：只截某个元素
 *
 * 使用方式：
 *   ScreenshotHelper helper = new ScreenshotHelper(driver);
 *   helper.captureViewport("登录页");        // 截当前屏幕
 *   helper.captureFullPage("首页");          // 截全页面
 *   helper.captureElement(By.id("nav"), "导航栏");  // 截某个元素
 */
public class ScreenshotHelper {

    private final WebDriver driver;
    private static final String SCREENSHOT_DIR = ConfigManager.get().screenshotDir();
    private static final ThreadLocal<String> currentTestName = new ThreadLocal<>();

    public ScreenshotHelper(WebDriver driver) {
        this.driver = driver;
        ensureDirExists(SCREENSHOT_DIR);  // 确保截图目录存在
    }

    /** 设置当前测试名称（用于截图文件名）。 */
    public static void setCurrentTest(String testName) {
        currentTestName.set(testName);
    }

    // ========== 三种截图方式 ==========

    /**
     * 截取整个页面（自动滚动拼接）。
     * 适合用来截长页面，看到所有内容。
     */
    public File captureFullPage(String testName) {
        try {
            // 用 JS 获取页面完整宽高
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Long height = (Long) js.executeScript(
                    "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)");
            Long width = (Long) js.executeScript(
                    "return Math.max(document.body.scrollWidth, document.documentElement.scrollWidth)");

            // 先把浏览器窗口设置成页面大小，才能截全
            org.openqa.selenium.Dimension originalSize = driver.manage().window().getSize();
            driver.manage().window().setSize(new org.openqa.selenium.Dimension(
                    width.intValue(), height.intValue()));

            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String filename = saveScreenshot(screenshot, testName, "fullpage");

            // 恢复窗口大小
            driver.manage().window().setSize(originalSize);

            return new File(filename);
        } catch (Exception e) {
            LogUtils.warn(getClass(), "全页截图失败：{}，改用视口截图", e.getMessage());
            return captureViewport(testName);
        }
    }

    /**
     * 截取当前视口（只截屏幕上能看到的部分）。
     * 一般测试失败时用这个就够了。
     */
    public File captureViewport(String testName) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String filename = saveScreenshot(screenshot, testName, "viewport");
            return new File(filename);
        } catch (Exception e) {
            LogUtils.error(getClass(), "视口截图失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 截取页面上的某个元素。
     * 比如只想截一个弹窗、一个表单区域。
     */
    public File captureElement(By locator, String testName) {
        try {
            WebElement element = driver.findElement(locator);
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            BufferedImage fullImg = ImageIO.read(screenshot);
            Point point = element.getLocation();
            int width = element.getSize().getWidth();
            int height = element.getSize().getHeight();

            // 从大图中裁剪出元素部分
            BufferedImage elementScreenshot = fullImg.getSubimage(
                    point.getX(), point.getY(), width, height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(elementScreenshot, "png", baos);

            String timestamp = DateUtils.timestamp();
            String filename = SCREENSHOT_DIR + "/" + testName + "_element_" + timestamp + ".png";
            Path path = Paths.get(filename);
            Files.write(path, baos.toByteArray());
            LogUtils.info(getClass(), "📸 元素截图已保存：{}", filename);
            return path.toFile();
        } catch (Exception e) {
            LogUtils.error(getClass(), "元素截图失败：{}", e.getMessage());
            return captureViewport(testName);
        }
    }

    /** 通用截图方法（根据配置决定截不截）。 */
    public File takeScreenshot(String testName) {
        if (ConfigManager.get().screenshotOnFailure()) {
            return captureViewport(testName);
        }
        return null;
    }

    /**
     * 截图并附加到 Allure 报告。
     * 测试失败时会自动调用这个。
     */
    @Attachment(value = "失败截图", type = "image/png")
    public byte[] attachScreenshotToAllure() {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    // ========== 内部方法 ==========

    /** 保存截图文件。 */
    private String saveScreenshot(File screenshot, String testName, String type) {
        try {
            String timestamp = DateUtils.timestamp();
            String filename = SCREENSHOT_DIR + "/" + testName + "_" + type + "_" + timestamp + ".png";
            FileHandler.copy(screenshot, new File(filename));
            LogUtils.info(getClass(), "📸 截图已保存：{}", filename);
            return filename;
        } catch (Exception e) {
            LogUtils.error(getClass(), "保存截图失败：{}", e.getMessage());
            return null;
        }
    }

    /** 确保目录存在。不存在就创建。 */
    private void ensureDirExists(String dir) {
        try {
            Files.createDirectories(Paths.get(dir));
        } catch (Exception e) {
            LogUtils.warn(getClass(), "无法创建目录 {}：{}", dir, e.getMessage());
        }
    }
}
