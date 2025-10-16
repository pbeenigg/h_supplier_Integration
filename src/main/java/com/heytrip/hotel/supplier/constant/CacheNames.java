package com.heytrip.hotel.supplier.constant;

/**
 * 静态数据缓存名称常量
 */
public final class CacheNames {
    private CacheNames() {
    }

    // 国家
    public static final String COUNTRY = "static:country";
    // 城市
    public static final String CITY = "static:city";
    // 酒店
    public static final String HOTEL = "static:hotel";
    // 房型
    public static final String ROOM = "static:room";
    // 价格计划
    public static final String RATE_PLAN = "static:rateplan";
    // 国籍
    public static final String NATIONALITY = "static:nationality";
    // GIATA
    public static final String GIATA = "static:giata";

    // 供应商配置缓存
    public static final String SUPPLIER_CONFIG_CACHE = "supplier:config";
    // 供应商认证配置缓存
    public static final String SUPPLIER_AUTH_CACHE = "supplier:supplier:auth";
    // 供应商FTP配置缓存
    public static final String SUPPLIER_AUTH_FTP = "supplier:supplier:ftp";


    // 系统用户缓存
    public static final String USER = "system:user";
    // 系统应用缓存
    public static final String APP = "system:app";
    // 系统配置缓存
    public static final String SYSTEM_CONFIG = "system:config";

}
