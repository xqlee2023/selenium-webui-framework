package com.framework.browser.provider;

import com.framework.enums.BrowserType;
import org.openqa.selenium.WebDriver;

import java.net.URL;
import com.framework.config.FrameworkConfig;
import com.framework.core.ConfigManager;

/**
 * ===========================================
 * 🏭 浏览器提供者接口 (OCP 核心)
 * ===========================================
 *
 * 遵循开闭原则：加新浏览器 = 实现此接口 + 注册一行。
 * BrowserFactory 不需要任何改动。
 */
public interface BrowserProvider {

    /** 支持的浏览器类型 */
    BrowserType browserType();

    /** 创建本地浏览器 */
    WebDriver createLocal(ConfigManager cfg);

    /** 创建远程浏览器（Selenium Grid） */
    WebDriver createRemote(URL hubUrl, ConfigManager cfg);
}
