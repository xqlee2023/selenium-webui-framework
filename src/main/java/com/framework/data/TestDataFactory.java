package com.framework.data;

import com.framework.data.model.Customer;
import com.framework.data.model.Order;
import com.framework.data.model.Product;
import net.datafaker.Faker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ===========================================
 * 🏭 测试数据工厂 — 一键生成中文测试数据
 * ===========================================
 *
 * 基于 DataFaker（Java Faker 后继者）封装，内置中文 locale。
 * 支持生成客户、产品、订单等常见业务实体。
 *
 * <h3>快速上手</h3>
 * <pre>{@code
 * // 单个客户
 * Customer c = TestDataFactory.customer();
 *
 * // 批量客户
 * List<Customer> cs = TestDataFactory.customers(10);
 *
 * // 单个产品
 * Product p = TestDataFactory.product();
 *
 * // 自定义产品
 * Product p2 = TestDataFactory.product()
 *     .toBuilder()
 *     .name("iPhone 15 Pro")
 *     .price(8999.00)
 *     .build();
 *
 * // 完整订单
 * Order o = TestDataFactory.order();
 * }</pre>
 *
 * @author Lee
 * @since 3.2.0
 */
public final class TestDataFactory {

    private static final Faker faker = new Faker(Locale.CHINA);
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TestDataFactory() {}

    // ══════════ 客户 ══════════

    /** 生成随机客户 */
    public static Customer customer() {
        return Customer.builder()
                .name(faker.name().fullName())
                .phone(faker.phoneNumber().cellPhone())
                .email(faker.internet().emailAddress())
                .idCard(faker.idNumber().validZhCNSsn())
                .company(faker.company().name())
                .province(faker.address().state())
                .city(faker.address().city())
                .address(faker.address().streetAddress())
                .build();
    }

    /** 生成指定数量的客户 */
    public static List<Customer> customers(int count) {
        List<Customer> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(customer());
        }
        return list;
    }

    /** 生成 VIP 客户（固定姓名 + 随机数据） */
    public static Customer vipCustomer(String name) {
        return Customer.builder()
                .name(name)
                .phone("138" + faker.number().digits(8))
                .email(name.replaceAll("\\s", "").toLowerCase() + "@vip.com")
                .idCard(faker.idNumber().validZhCNSsn())
                .company(faker.company().name())
                .province("上海市")
                .city("浦东新区")
                .address(faker.address().streetAddress())
                .build();
    }

    // ══════════ 产品 ══════════

    private static final String[] CATEGORIES = {
            "电子产品", "服装鞋帽", "食品饮料", "家居用品", "运动户外",
            "美妆个护", "图书音像", "母婴用品", "汽车用品", "办公文具"
    };

    private static final String[] BRANDS = {
            "华为", "小米", "苹果", "三星", "联想", "海尔", "格力",
            "美的", "OPPO", "vivo", "安踏", "李宁", "飞利浦", "索尼"
    };

    /** 生成随机产品 */
    public static Product product() {
        return Product.builder()
                .name(faker.commerce().productName())
                .sku("SKU-" + faker.number().digits(10).toUpperCase())
                .category(randomFrom(CATEGORIES))
                .price(Math.round(ThreadLocalRandom.current().nextDouble(9.9, 9999.0) * 100.0) / 100.0)
                .stock(ThreadLocalRandom.current().nextInt(0, 5000))
                .description(faker.lorem().sentence(8))
                .brand(randomFrom(BRANDS))
                .barcode(faker.code().ean13())
                .build();
    }

    /** 生成指定数量的产品 */
    public static List<Product> products(int count) {
        List<Product> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(product());
        }
        return list;
    }

    /** 按类别生成产品 */
    public static Product productByCategory(String category) {
        return product().toBuilder().category(category).build();
    }

    // ══════════ 订单 ══════════

    private static final String[] ORDER_STATUS = {
            "待付款", "待发货", "已发货", "已完成", "已取消"
    };

    private static final String[] PAYMENT_METHODS = {
            "微信支付", "支付宝", "银行卡", "货到付款"
    };

    /** 生成随机订单（含客户和产品） */
    public static Order order() {
        Customer c = customer();
        Product p = product();
        int qty = ThreadLocalRandom.current().nextInt(1, 5);
        return Order.builder()
                .orderNo("ORD" + System.currentTimeMillis() + faker.number().digits(4))
                .customer(c)
                .product(p)
                .quantity(qty)
                .totalAmount(Math.round(p.getPrice() * qty * 100.0) / 100.0)
                .status(randomFrom(ORDER_STATUS))
                .paymentMethod(randomFrom(PAYMENT_METHODS))
                .createDate(LocalDate.now().format(DF))
                .build();
    }

    /** 生成指定数量的订单 */
    public static List<Order> orders(int count) {
        List<Order> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(order());
        }
        return list;
    }

    // ══════════ 通用工具方法 ══════════

    /** 随机手机号 */
    public static String phone() {
        return faker.phoneNumber().cellPhone();
    }

    /** 随机中文姓名 */
    public static String chineseName() {
        return faker.name().fullName();
    }

    /** 随机中文地址 */
    public static String address() {
        return faker.address().fullAddress();
    }

    /** 随机邮箱 */
    public static String email() {
        return faker.internet().emailAddress();
    }

    /** 随机公司名 */
    public static String company() {
        return faker.company().name();
    }

    /** 随机 UUID */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /** 前 N 天的日期字符串 */
    public static String daysAgo(int days) {
        return LocalDate.now().minusDays(days).format(DF);
    }

    /** 范围内的随机整数 */
    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /** 随机金额（两位小数） */
    public static double randomAmount(double min, double max) {
        return Math.round(ThreadLocalRandom.current().nextDouble(min, max) * 100.0) / 100.0;
    }

    @SafeVarargs
    private static <T> T randomFrom(T... array) {
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }
}
