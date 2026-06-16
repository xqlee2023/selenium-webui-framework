package com.framework.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户测试数据模型。
 *
 * @author Lee
 * @since 3.2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private String name;
    private String phone;
    private String email;
    private String idCard;
    private String company;
    private String address;
    private String city;
    private String province;

    /** 单行地址 */
    public String fullAddress() {
        return province + city + address;
    }
}
