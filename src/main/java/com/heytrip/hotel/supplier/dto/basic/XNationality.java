package com.heytrip.hotel.supplier.dto.basic;

import lombok.Data;

/**
 * 兼容 DTO：用于国籍静态数据返回
 * 说明：供应商标准包中暂无该类时，临时在本工程内提供同名语义的DTO。
 */
@Data
public class XNationality {
    private String nationalityCode;
    private String nationality;
    private String isoCode;


}
