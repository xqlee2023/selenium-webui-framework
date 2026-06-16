package com.framework.event;

import java.lang.annotation.*;

/**
 * 标记一个方法作为事件订阅者。
 * 被标记的方法必须只有一个参数（事件类型）。
 *
 * 用法：
 *   @Subscribe
 *   public void onTestFailure(TestFailureEvent event) { ... }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Subscribe {
}
