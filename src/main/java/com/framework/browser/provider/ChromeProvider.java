package com.framework.browser.provider;

import com.framework.enums.BrowserType;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import com.framework.browser.BrowserOptionApplier;
import com.framework.core.ConfigManager;

/**
 * Chrome 浏览器提供者。
 * 遵循 OCP：新浏览器只需实现 BrowserProvider，不碰 BrowserFactory。
 */
public class ChromeProvider implements BrowserProvider {

    private final BrowserOptionApplier optionApplier = new BrowserOptionApplier();

    @Override
    public BrowserType browserType() {
        return BrowserType.CHROME;
    }

    @Override
    public WebDriver createLocal(ConfigManager cfg) {
        WebDriverManager.chromedriver().clearDriverCache().setup();
        ChromeOptions options = new ChromeOptions();
        optionApplier.applyChrome(options, cfg);
        return new ChromeDriver(options);
    }

    @Override
    public WebDriver createRemote(URL hubUrl, ConfigManager cfg) {
        ChromeOptions options = new ChromeOptions();
        optionApplier.applyChrome(options, cfg);
        return new RemoteWebDriver(hubUrl, options);
    }
}
