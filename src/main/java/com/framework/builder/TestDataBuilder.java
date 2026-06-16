package com.framework.builder;

import java.util.HashMap;
import java.util.Map;

/**
 * ===========================================
 * 🏗️ 测试数据构建器 (Builder Pattern)
 * ===========================================
 *
 * 遵循 建造者模式：分步构建测试数据对象。
 * 避免构造器参数过多、避免大量重载方法。
 *
 * 使用方式：
 *   Map<String, String> user = TestDataBuilder.aUser()
 *       .withUsername("admin")
 *       .withPassword("Admin123!")
 *       .withEmail("admin@test.com")
 *       .withRole("admin")
 *       .build();
 */
public class TestDataBuilder {

    private final Map<String, String> data = new HashMap<>();

    private TestDataBuilder() {}

    /** 创建用户测试数据 */
    public static TestDataBuilder aUser() {
        return new TestDataBuilder();
    }

    /** 创建订单测试数据 */
    public static TestDataBuilder anOrder() {
        return new TestDataBuilder();
    }

    /** 创建搜索测试数据 */
    public static TestDataBuilder aSearch() {
        return new TestDataBuilder();
    }

    // ========== 通用字段 ==========

    public TestDataBuilder with(String key, String value) {
        data.put(key, value);
        return this;
    }

    // ========== 用户相关 ==========

    public TestDataBuilder withUsername(String username) {
        data.put("username", username);
        return this;
    }

    public TestDataBuilder withPassword(String password) {
        data.put("password", password);
        return this;
    }

    public TestDataBuilder withEmail(String email) {
        data.put("email", email);
        return this;
    }

    public TestDataBuilder withRole(String role) {
        data.put("role", role);
        return this;
    }

    /** 标记用户为已锁定 */
    public TestDataBuilder locked() {
        data.put("status", "locked");
        return this;
    }

    // ========== 订单相关 ==========

    public TestDataBuilder withProduct(String product) {
        data.put("product", product);
        return this;
    }

    public TestDataBuilder withQuantity(int qty) {
        data.put("quantity", String.valueOf(qty));
        return this;
    }

    public TestDataBuilder withAmount(String amount) {
        data.put("amount", amount);
        return this;
    }

    // ========== 构建 ==========

    /** 构建为 Map（TestNG DataProvider 友好格式） */
    public Map<String, String> build() {
        return new HashMap<>(data);
    }

    /** 构建为 Object[]（@DataProvider 返回值格式） */
    public Object[] buildAsRow() {
        return new Object[]{build()};
    }

    /** 获取单个字段值 */
    public String get(String key) {
        return data.get(key);
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
