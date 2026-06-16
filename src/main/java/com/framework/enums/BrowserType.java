package com.framework.enums;

/** Supported browser types. */
public enum BrowserType {
    CHROME,
    FIREFOX,
    EDGE,
    SAFARI;

    public static BrowserType fromString(String name) {
        for (BrowserType b : values()) {
            if (b.name().equalsIgnoreCase(name)) return b;
        }
        return CHROME;
    }
}
