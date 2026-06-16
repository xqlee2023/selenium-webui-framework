package com.framework.browser.provider;

import com.framework.enums.BrowserType;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import com.framework.browser.BrowserOptionApplier;
import com.framework.core.ConfigManager;

/** Edge 浏览器提供者。 */
public class EdgeProvider implements BrowserProvider {

    private final BrowserOptionApplier optionApplier = new BrowserOptionApplier();

    @Override
    public BrowserType browserType() {
        return BrowserType.EDGE;
    }

    @Override
    public WebDriver createLocal(ConfigManager cfg) {
        WebDriverManager.edgedriver().clearDriverCache().setup();
        EdgeOptions options = new EdgeOptions();
        optionApplier.applyEdge(options, cfg);
        return new EdgeDriver(options);
    }

    @Override
    public WebDriver createRemote(URL hubUrl, ConfigManager cfg) {
        EdgeOptions options = new EdgeOptions();
        optionApplier.applyEdge(options, cfg);
        return new RemoteWebDriver(hubUrl, options);
    }
}
