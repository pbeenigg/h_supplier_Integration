package com.heytrip.hotel.supplier.adapter.builder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 标记字段在构建查询参数时需要以 JSON 字符串形式序列化
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface JsonParam {
}
