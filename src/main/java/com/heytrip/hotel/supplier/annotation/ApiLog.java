package com.heytrip.hotel.supplier.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API调用日志记录注解
 * 用于标记需要记录日志的Controller方法
 *
 * @author Pax
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiLog {

    /**
     * 业务类型
     * 例如：search、booking、cancel、getPrice、getHotel等
     */
    String businessType() default "";

    /**
     * 是否记录订单详细日志到distribution_orders_log表
     * 默认false，仅记录到distribution_call_log表
     */
    boolean recordOrderDetail() default false;

    /**
     * 需要从请求中提取的业务字段
     * 例如：{"hotelKey", "checkInKey", "distributionOrdersKey"}
     */
    String[] extractFields() default {};

    /**
     * 描述信息
     */
    String description() default "";

    /**
     * 是否启用日志记录
     * 默认true，可通过配置动态控制
     */
    boolean enabled() default true;
}
