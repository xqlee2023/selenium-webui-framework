package com.framework.event;

import com.framework.utils.LogUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * ===========================================
 * 📡 事件总线 (EventBus) — 单例模式
 * ===========================================
 *
 * 轻量级事件分发系统，取代 TestListener 中的硬编码 Hook 列表。
 * 组件通过 @Subscribe 注解或 lambda 注册，实现完全解耦。
 *
 * 设计原则：
 *   - 发布-订阅模式：发布者和订阅者互不知晓
 *   - 线程安全：支持并发发布
 *   - 同步分发：同一线程内保证事件顺序
 *   - 优雅降级：单个订阅者异常不影响其他订阅者
 *
 * 使用方式：
 *   // 注册 lambda 订阅者
 *   EventBus.register(TestFailureEvent.class, event -> {
 *       ScreenshotHook.capture(event.getResult());
 *   });
 *
 *   // 使用 @Subscribe 注解
 *   EventBus.register(new MyHook());
 *
 *   // 发布事件
 *   EventBus.post(new TestFailureEvent(result));
 */
public final class EventBus {

    private static volatile EventBus instance;

    /** 事件类型 → 订阅者列表 */
    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Subscriber>> subscribers;

    private EventBus() {
        this.subscribers = new ConcurrentHashMap<>();
    }

    // ========== 单例 ==========

    public static EventBus getInstance() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) {
                    instance = new EventBus();
                    LogUtils.info(EventBus.class, "事件总线已初始化");
                }
            }
        }
        return instance;
    }

    // ========== 注册（lambda 方式） ==========

    /**
     * 注册一个 lambda 订阅者。
     *
     * @param eventType 事件类型
     * @param listener  事件处理函数
     * @param <T>       事件类型参数
     */
    public static <T> void register(Class<T> eventType, Consumer<T> listener) {
        getInstance().addSubscriber(eventType, new Subscriber(listener, null));
    }

    /**
     * 注册一个带标识的订阅者（方便注销）。
     *
     * @param eventType 事件类型
     * @param listener  事件处理函数
     * @param owner     拥有者标识
     * @param <T>       事件类型参数
     */
    public static <T> void register(Class<T> eventType, Consumer<T> listener, Object owner) {
        getInstance().addSubscriber(eventType, new Subscriber(listener, owner));
    }

    /**
     * 注册一个对象的所有 @Subscribe 方法。
     *
     * @param object 包含 @Subscribe 方法的对象
     */
    public static void register(Object object) {
        EventBus bus = getInstance();
        for (Method method : object.getClass().getMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) continue;

            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                LogUtils.warn(EventBus.class,
                        "@Subscribe 方法 {} 必须只有一个参数", method.getName());
                continue;
            }

            Class<?> eventType = paramTypes[0];
            Consumer<Object> listener = event -> {
                try {
                    method.invoke(object, event);
                } catch (Exception e) {
                    LogUtils.error(EventBus.class,
                            "事件处理异常 {}: {}", method.getName(), e.getMessage());
                }
            };
            bus.addSubscriber(eventType, new Subscriber(listener, object));
        }
    }

    // ========== 注销 ==========

    /**
     * 注销某个拥有者的所有订阅。
     */
    public static void unregister(Object owner) {
        EventBus bus = getInstance();
        int count = 0;
        for (CopyOnWriteArrayList<Subscriber> list : bus.subscribers.values()) {
            if (list.removeIf(sub -> owner.equals(sub.owner))) count++;
        }
        if (count > 0) {
            LogUtils.info(EventBus.class, "已注销 {} 个订阅 ({})", count, owner.getClass().getSimpleName());
        }
    }

    // ========== 发布 ==========

    /**
     * 发布事件到所有匹配的订阅者。
     * 同步分发：保证同一线程内事件顺序。
     *
     * @param event 事件对象
     * @param <T>   事件类型
     */
    @SuppressWarnings("unchecked")
    public static <T> void post(T event) {
        EventBus bus = getInstance();
        Class<?> eventType = event.getClass();

        CopyOnWriteArrayList<Subscriber> list = bus.subscribers.get(eventType);
        if (list == null || list.isEmpty()) return;

        for (Subscriber subscriber : list) {
            try {
                ((Consumer<T>) subscriber.listener).accept(event);
            } catch (Exception e) {
                LogUtils.error(EventBus.class,
                        "事件处理异常 [{}]: {}", eventType.getSimpleName(), e.getMessage());
            }
        }
    }

    // ========== 内部方法 ==========

    private void addSubscriber(Class<?> eventType, Subscriber subscriber) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(subscriber);
    }

    // ========== 内部类 ==========

    private static class Subscriber {
        final Object listener;
        final Object owner;

        Subscriber(Object listener, Object owner) {
            this.listener = listener;
            this.owner = owner;
        }
    }
}
