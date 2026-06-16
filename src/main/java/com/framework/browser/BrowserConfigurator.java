package com.framework.browser;

import com.framework.core.ConfigManager;

import com.framework.utils.LogUtils;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

/**
 * ===========================================
 * ⚙️ 浏览器配置器 — 单一职责
 * ===========================================
 *
 * 只负责对新创建的 WebDriver 做通用配置：
 * - 设置隐式等待
 * - 设置页面加载超时
 * - 最大化窗口
 */
public class BrowserConfigurator {

    void configure(WebDriver driver, ConfigManager cfg) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(cfg.implicitWait()));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(cfg.pageLoadWait()));

        if (cfg.maximize()) {
            try {
                driver.manage().window().maximize();
            } catch (Exception ignored) {
                // 无头模式或远程可能不支持最大化
            }
        }

        LogUtils.info(getClass(), "🚀 浏览器已启动: {} | 模式: {} | 无头: {}",
                cfg.browser(), cfg.executionMode(), cfg.headless());
    }
}
