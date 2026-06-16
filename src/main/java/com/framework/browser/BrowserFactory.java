package com.framework.browser;

import com.framework.browser.provider.BrowserProvider;
import com.framework.core.ConfigManager;
import com.framework.enums.BrowserType;
import com.framework.utils.LogUtils;
import org.openqa.selenium.WebDriver;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.framework.browser.provider.ChromeProvider;
import com.framework.browser.provider.EdgeProvider;
import com.framework.browser.provider.FirefoxProvider;
import com.framework.browser.provider.SafariProvider;

/**
 * ===========================================
 * 🏭 浏览器工厂 (BrowserFactory) — Provider 注册表
 * ===========================================
 *
 * 遵循 OCP：对新浏览器扩展开放，对修改关闭。
 * 加新浏览器 = 实现 BrowserProvider + 一行 register()。
 *
 * 支持：
 *   - 本地浏览器：Chrome / Firefox / Edge / Safari
 *   - 远程浏览器：Selenium Grid / Docker / 云服务
 *   - 运行时注册自定义浏览器
 */
public class BrowserFactory {

    private static final Map<BrowserType, BrowserProvider> providers = new ConcurrentHashMap<>();
    private static final BrowserConfigurator configurator = new BrowserConfigurator();

    static {
        register(new ChromeProvider());
        register(new FirefoxProvider());
        register(new EdgeProvider());
        register(new SafariProvider());
        // 加 Brave？加一行：register(new BraveProvider());
    }

    private BrowserFactory() {}

    /**
     * 注册浏览器提供者。
     * 可在运行时动态注册（如通过 SPI 发现的第三方浏览器）。
     */
    public static void register(BrowserProvider provider) {
        providers.put(provider.browserType(), provider);
        LogUtils.info(BrowserFactory.class, "📦 已注册浏览器提供者: {}", provider.getClass().getSimpleName());
    }

    /**
     * 创建 WebDriver（根据当前配置自动选择）。
     *
     * @return 配置好的 WebDriver
     */
    public static WebDriver createDriver() {
        ConfigManager cfg = ConfigManager.get();
        BrowserProvider provider = providers.get(cfg.browser());

        if (provider == null) {
            throw new IllegalStateException(
                    "未找到浏览器提供者: " + cfg.browser() +
                    "。已注册: " + providers.keySet());
        }

        WebDriver driver = switch (cfg.executionMode()) {
            case LOCAL -> provider.createLocal(cfg);
            case REMOTE, GRID, DOCKER, CLOUD -> {
                try {
                    yield provider.createRemote(new URL(cfg.hubUrl()), cfg);
                } catch (Exception e) {
                    throw new RuntimeException("连接远程 Grid 失败: " + cfg.hubUrl(), e);
                }
            }
        };

        configurator.configure(driver, cfg);
        return driver;
    }

    /** 查询已注册的浏览器类型。 */
    public static Map<BrowserType, BrowserProvider> registeredProviders() {
        return Map.copyOf(providers);
    }
}
