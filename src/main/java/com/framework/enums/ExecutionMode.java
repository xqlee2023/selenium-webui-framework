package com.framework.enums;

public enum ExecutionMode {
    LOCAL,
    REMOTE,
    GRID,
    DOCKER,
    CLOUD;

    public static ExecutionMode fromString(String name) {
        for (ExecutionMode m : values()) {
            if (m.name().equalsIgnoreCase(name)) return m;
        }
        return LOCAL;
    }
}
