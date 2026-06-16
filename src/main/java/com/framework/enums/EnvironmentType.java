package com.framework.enums;

public enum EnvironmentType {
    DEV,
    QA,
    STAGING,
    PRODUCTION;

    public static EnvironmentType fromString(String name) {
        for (EnvironmentType e : values()) {
            if (e.name().equalsIgnoreCase(name)) return e;
        }
        return DEV;
    }
}
