package com.framework.browser;

import com.framework.core.ConfigManager;

import com.framework.utils.LogUtils;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;
import java.util.Map;

/**
 * ===========================================
 * 🔧 浏览器选项配置器 — 单一职责
 * ===========================================
 *
 * 从 BrowserFactory 中拆分出来，只负责给 Browser Options 应用通用配置。
 * 不管是本地还是远程，都用同一套配置。
 */
public class BrowserOptionApplier {

    /**
     * 应用 Chrome 通用配置：headless、maximize、禁用 GPU、自定义参数等。
     */
    public void applyChrome(ChromeOptions options, ConfigManager cfg) {
        if (cfg.headless()) {
            options.addArguments("--headless=new");
        }
        if (cfg.maximize()) {
            options.addArguments("--start-maximized");
        }
        options.addArguments("--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("--disable-notifications", "--disable-popup-blocking");

        for (String arg : cfg.browserArguments()) {
            options.addArguments(arg);
        }
        if (!cfg.browserPreferences().isEmpty()) {
            for (Map.Entry<String, Object> entry : cfg.browserPreferences().entrySet()) {
                options.setExperimentalOption(entry.getKey(), entry.getValue());
            }
        }
        options.setPageLoadTimeout(Duration.ofSeconds(cfg.pageLoadWait()));
    }

    /**
     * 应用 Edge 通用配置。
     */
    public void applyEdge(EdgeOptions options, ConfigManager cfg) {
        if (cfg.headless()) options.addArguments("--headless");
        if (cfg.maximize()) options.addArguments("--start-maximized");
        options.addArguments("--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        options.setPageLoadTimeout(Duration.ofSeconds(cfg.pageLoadWait()));
    }

    /**
     * 应用 Firefox 通用配置。
     */
    public void applyFirefox(FirefoxOptions options, ConfigManager cfg) {
        if (cfg.headless()) options.addArguments("--headless");
        options.setPageLoadTimeout(Duration.ofSeconds(cfg.pageLoadWait()));
    }
}
