package com.framework.annotations;

import java.lang.annotation.*;

/**
 * 页面元素描述注解 — 配合 @FindBy 使用
 *
 * Selenium 的 @FindBy 只管定位，没有业务语义。
 * @Desc 补上这个缺口，让元素有"名字"，可用于日志、报告、AI 上下文。
 *
 * <h3>用法</h3>
 * <pre>{@code
 * @FinDescdBy(id = "username")
 * @Desc("用户名输入框")
 * private WebElement usernameInput;
 * }</pre>
 *
 * @author Lee
 * @since 3.2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Desc {
    /** 元素业务描述 */
    String value();
}
