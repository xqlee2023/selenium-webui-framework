package com.framework.config.browser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 浏览器选项配置。 */
public class BrowserOptionsConfig {
    public Boolean headless = false;
    public Boolean maximize = true;
    public List<String> arguments = new ArrayList<>();
    public Map<String, Object> preferences = new HashMap<>();
}
