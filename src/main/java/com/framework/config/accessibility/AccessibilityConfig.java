package com.framework.config.accessibility;

/** 无障碍检测配置。 */
public class AccessibilityConfig {
    /** 是否启用自动无障碍扫描 */
    public boolean enabled = true;
    /** 仅在测试成功后扫描（失败时页面可能已损坏） */
    public boolean onSuccessOnly = false;
    /** 违规严重度最低阈值（critical / serious / moderate / minor） */
    public String minImpactLevel = "serious";
    /** 最大违规数（超过则告警） */
    public int maxViolations = 50;
}
