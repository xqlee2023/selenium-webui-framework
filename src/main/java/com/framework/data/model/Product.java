package com.framework.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 产品测试数据模型。
 *
 * @author Lee
 * @since 3.2.0
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String name;
    private String sku;
    private String category;
    private double price;
    private int stock;
    private String description;
    private String brand;
    private String barcode;
}
