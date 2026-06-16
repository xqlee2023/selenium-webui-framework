package com.framework.browser.provider;

import com.framework.enums.BrowserType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.net.URL;
import com.framework.core.ConfigManager;

/** Safari 浏览器提供者（需 macOS + Safari 开启远程自动化）。 */
public class SafariProvider implements BrowserProvider {

    @Override
    public BrowserType browserType() {
        return BrowserType.SAFARI;
    }

    @Override
    public WebDriver createLocal(ConfigManager cfg) {
        SafariOptions options = new SafariOptions();
        return new SafariDriver(options);
    }

    @Override
    public WebDriver createRemote(URL hubUrl, ConfigManager cfg) {
        SafariOptions options = new SafariOptions();
        return new RemoteWebDriver(hubUrl, options);
    }
}
