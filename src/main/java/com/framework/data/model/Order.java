package com.framework.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单测试数据模型。
 *
 * @author Lee
 * @since 3.2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String orderNo;
    private Customer customer;
    private Product product;
    private int quantity;
    private double totalAmount;
    private String status;
    private String paymentMethod;
    private String createDate;
}
