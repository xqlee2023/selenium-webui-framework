package com.framework.browser.provider;

import com.framework.enums.BrowserType;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import com.framework.browser.BrowserOptionApplier;
import com.framework.core.ConfigManager;

/** Firefox 浏览器提供者。 */
public class FirefoxProvider implements BrowserProvider {

    private final BrowserOptionApplier optionApplier = new BrowserOptionApplier();

    @Override
    public BrowserType browserType() {
        return BrowserType.FIREFOX;
    }

    @Override
    public WebDriver createLocal(ConfigManager cfg) {
        WebDriverManager.firefoxdriver().clearDriverCache().setup();
        FirefoxOptions options = new FirefoxOptions();
        optionApplier.applyFirefox(options, cfg);
        return new FirefoxDriver(options);
    }

    @Override
    public WebDriver createRemote(URL hubUrl, ConfigManager cfg) {
        FirefoxOptions options = new FirefoxOptions();
        optionApplier.applyFirefox(options, cfg);
        return new RemoteWebDriver(hubUrl, options);
    }
}
