package com.framework.config.execution;

/** 执行配置。 */
public class ExecutionConfig {
    public int parallelCount = 2;
    /** 等待策略类型: explicit, fluent, etc. (默认: explicit) */
    public String waitStrategy = "explicit";
}
