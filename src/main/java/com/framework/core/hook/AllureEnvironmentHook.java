package com.framework.core.hook;

import com.framework.utils.LogUtils;
import org.testng.ISuite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import com.framework.core.ConfigManager;

/**
 * ===========================================
 * 📋 Allure 环境信息 Hook
 * ===========================================
 *
 * Suite 启动时自动写入 environment.properties 到 allure-results。
 * Allure 报告的环境页会展示这些信息。
 *
 * <h3>写入内容</h3>
 * <ul>
 *   <li>操作系统名称 + 版本 + 架构</li>
 *   <li>Java 版本 + 供应商</li>
 *   <li>浏览器类型 + 版本（如有）</li>
 *   <li>屏幕分辨率（如有）</li>
 *   <li>执行时间</li>
 *   <li>框架版本</li>
 * </ul>
 *
 * @author Lee
 * @since 3.2.0
 */
public class AllureEnvironmentHook implements TestEventHook {

    private static final String ALLURE_RESULTS = "allure-results";

    @Override
    public void onSuiteStart(ISuite suite) {
        try {
            Properties env = buildEnvironmentInfo();
            writeProperties(env);
            LogUtils.info(getClass(), "📋 Allure 环境信息已写入");
        } catch (Exception e) {
            LogUtils.warn(getClass(), "写入 Allure 环境信息失败: {}", e.getMessage());
        }
    }

    private Properties buildEnvironmentInfo() {
        Properties props = new Properties();

        // 操作系统
        props.setProperty("OS.Name", System.getProperty("os.name"));
        props.setProperty("OS.Version", System.getProperty("os.version"));
        props.setProperty("OS.Arch", System.getProperty("os.arch"));

        // Java
        props.setProperty("Java.Version", System.getProperty("java.version"));
        props.setProperty("Java.Vendor", System.getProperty("java.vendor", "Unknown"));
        props.setProperty("Java.Home", System.getProperty("java.home"));

        // 浏览器（从系统属性读取，运行时可覆盖）
        String browser = System.getProperty("browser", ConfigManager.get().browser().name());
        props.setProperty("Browser", browser);

        // 环境
        props.setProperty("Environment", ConfigManager.get().environment().name());
        props.setProperty("Execution.Mode", ConfigManager.get().executionMode().name());

        // 框架
        props.setProperty("Framework.Version", "3.2.0");
        props.setProperty("Selenium.Version", "4.30.0");
        props.setProperty("TestNG.Version", "7.11.0");

        // Allure 结果目录
        props.setProperty("Allure.Results.Dir", ConfigManager.get().allureResultsDir());

        // 执行时间
        props.setProperty("Run.Timestamp", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 用户
        props.setProperty("User.Name", System.getProperty("user.name"));

        // 头显模式
        props.setProperty("Headless", System.getProperty("headless",
                String.valueOf(ConfigManager.get().headless())));

        return props;
    }

    private void writeProperties(Properties props) throws IOException {
        File dir = new File(ALLURE_RESULTS);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, "environment.properties");
        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, null);
        }
    }
}
