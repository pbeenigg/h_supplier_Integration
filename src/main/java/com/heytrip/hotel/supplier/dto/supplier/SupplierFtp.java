package com.heytrip.hotel.supplier.dto.supplier;

import lombok.Data;

/**
 * FTP 配置（来源于 SupplierConfig.ftpConfig JSON）
 * 示例JSON：
 * {
 *   "host":"18.170.183.159",
 *   "port":21,
 *   "username":"colosseum_live_static_data",
 *   "password":"***",
 *   "citiesPath":"/static_data_cities.csv",
 *   "countriesPath":"/static_data_countries.csv",
 *   "hotelsPath":"/static_data_hotels.csv",
 *   "nationalityPath":"/static_data_nationality.csv",
 *   "giataLocalPath":"classpath:csv/aosc_giata_id.csv"
 * }
 *
 * 通用打开方式：支持“类型,地址”的配置
 * - ftp,/path/to/file.csv -> 走 FTP 下载
 * - local,classpath:csv/file.csv 或 local,/data/file.csv -> 走本地读取
 * - 兼容：不含逗号时，defaultFtp=true 则按 FTP 拉取，否则按本地读取
 */
@Data
public class SupplierFtp {
    private String host;
    private Integer port = 21;
    private String username;
    private String password;

    // CSV 文件路径
    private String citiesPath;
    private String countriesPath;
    private String hotelsPath;
    private String nationalityPath;

    // GIATA 文件走本地导入
    private String giataLocalPath; // 支持 classpath: 前缀
}
