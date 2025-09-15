-- MySQL 8 DDL：静态数据相关表结构
-- 注意：根据 JPA 注解推导，字段长度/索引名称已尽量保持一致；如需调整请与 DBA 确认。
-- 字符集统一采用 utf8mb4，排序规则 utf8mb4_general_ci

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. country
DROP TABLE IF EXISTS `country`;
CREATE TABLE `country` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `supplier_code` VARCHAR(100) NOT NULL COMMENT '供应商代码',
  `country_code` VARCHAR(50) NOT NULL COMMENT '国家代码',
  `country_name` VARCHAR(200) NULL COMMENT '国家名称',
  `created_at` DATETIME NULL COMMENT '创建时间',
  `updated_at` DATETIME NULL COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_country_supplier_code` (`supplier_id`, `supplier_code`, `country_code`),
  KEY `idx_country_supplier` (`supplier_id`, `supplier_code`),
  KEY `idx_country_code` (`country_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-国家';

-- 2. city
DROP TABLE IF EXISTS `city`;
CREATE TABLE `city` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `supplier_code` VARCHAR(100) NOT NULL COMMENT '供应商代码',
  `city_code` VARCHAR(50) NOT NULL COMMENT '城市代码',
  `name` VARCHAR(200) NULL COMMENT '城市名称',
  `country_code` VARCHAR(50) NULL COMMENT '国家代码',
  `country_name` VARCHAR(200) NULL COMMENT '国家名称',
  `created_at` DATETIME NULL COMMENT '创建时间',
  `updated_at` DATETIME NULL COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_city_supplier_code` (`supplier_id`, `supplier_code`, `city_code`),
  KEY `idx_city_supplier` (`supplier_id`, `supplier_code`),
  KEY `idx_city_code` (`city_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-城市';

-- 3. hotel
DROP TABLE IF EXISTS `hotel`;
CREATE TABLE `hotel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `supplier_code` VARCHAR(100) NOT NULL COMMENT '供应商代码',
  `hotel_code` VARCHAR(64) NOT NULL COMMENT '酒店编码/ID',
  `name` VARCHAR(300) NULL COMMENT '酒店名称',
  `city_code` VARCHAR(50) NULL COMMENT '城市代码',
  `city_name` VARCHAR(200) NULL COMMENT '城市名称',
  `country_code` VARCHAR(50) NULL COMMENT '国家代码',
  `main_image` VARCHAR(500) NULL COMMENT '主图URL',
  `short_desc` TEXT NULL COMMENT '简述',
  `long_desc` LONGTEXT NULL COMMENT '描述',
  `address` VARCHAR(500) NULL COMMENT '地址',
  `phone` VARCHAR(100) NULL COMMENT '电话',
  `website` VARCHAR(200) NULL COMMENT '官网',
  `latitude` DOUBLE NULL COMMENT '纬度',
  `longitude` DOUBLE NULL COMMENT '经度',
  `rating` DOUBLE NULL COMMENT '评分/星级',
  `created_at` DATETIME NULL COMMENT '创建时间',
  `updated_at` DATETIME NULL COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hotel_supplier_code` (`supplier_id`, `supplier_code`, `hotel_code`),
  KEY `idx_hotel_supplier` (`supplier_id`, `supplier_code`),
  KEY `idx_hotel_code` (`hotel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-酒店';

-- 4. room
DROP TABLE IF EXISTS `room`;
CREATE TABLE `room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `supplier_code` VARCHAR(100) NOT NULL COMMENT '供应商代码',
  `room_code` VARCHAR(64) NOT NULL COMMENT '房型编码',
  `hotel_code` VARCHAR(64) NULL COMMENT '所属酒店编码',
  `name` VARCHAR(300) NULL COMMENT '房型名称',
  `bed_type` VARCHAR(100) NULL COMMENT '床型',
  `area` VARCHAR(100) NULL COMMENT '面积',
  `occupancy` INT NULL COMMENT '最大入住人数',
  `created_at` DATETIME NULL COMMENT '创建时间',
  `updated_at` DATETIME NULL COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_supplier_code` (`supplier_id`, `supplier_code`, `room_code`),
  KEY `idx_room_supplier` (`supplier_id`, `supplier_code`),
  KEY `idx_room_code` (`room_code`),
  KEY `idx_room_hotel` (`hotel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-房型';

-- 5. rate_plan
DROP TABLE IF EXISTS `rate_plan`;
CREATE TABLE `rate_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `supplier_code` VARCHAR(100) NOT NULL COMMENT '供应商代码',
  `rate_plan_code` VARCHAR(64) NOT NULL COMMENT '价格计划编码',
  `hotel_code` VARCHAR(64) NULL COMMENT '所属酒店编码',
  `room_code` VARCHAR(64) NULL COMMENT '所属房型编码',
  `name` VARCHAR(300) NULL COMMENT '价格计划名称',
  `meal` VARCHAR(100) NULL COMMENT '餐食类型',
  `cancellation_policy` TEXT NULL COMMENT '取消政策',
  `created_at` DATETIME NULL COMMENT '创建时间',
  `updated_at` DATETIME NULL COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rateplan_supplier_code` (`supplier_id`, `supplier_code`, `rate_plan_code`),
  KEY `idx_rateplan_supplier` (`supplier_id`, `supplier_code`),
  KEY `idx_rateplan_code` (`rate_plan_code`),
  KEY `idx_rateplan_hotel` (`hotel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-价格计划';

-- 6. nationality
DROP TABLE IF EXISTS `nationality`;
CREATE TABLE `nationality` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `supplier_code` VARCHAR(100) NOT NULL COMMENT '供应商代码',
  `nationality_code` VARCHAR(50) NOT NULL COMMENT '国籍代码',
  `nationality` VARCHAR(200) NULL COMMENT '国籍名称',
  `iso_code` VARCHAR(10) NULL COMMENT 'ISO代码',
  `created_at` DATETIME NULL COMMENT '创建时间',
  `updated_at` DATETIME NULL COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_nat_supplier_code` (`supplier_id`, `supplier_code`, `nationality_code`),
  KEY `idx_nat_supplier` (`supplier_id`, `supplier_code`),
  KEY `idx_nat_code` (`nationality_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-国籍';

-- 7. hotel_giata
DROP TABLE IF EXISTS `hotel_giata`;
CREATE TABLE `hotel_giata` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `supplier_code` VARCHAR(100) NOT NULL COMMENT '供应商代码',
  `hotel_code` VARCHAR(64) NOT NULL COMMENT '酒店编码/ID',
  `giata_id` VARCHAR(64) NOT NULL COMMENT 'GIATA 编码',
  `name` VARCHAR(300) NULL COMMENT '酒店名称',
  `city_code` VARCHAR(50) NULL COMMENT '城市代码',
  `city_name` VARCHAR(200) NULL COMMENT '城市名称',
  `country_code` VARCHAR(20) NULL COMMENT '国家代码',
  `long_desc` LONGTEXT NULL COMMENT '描述',
  `latitude` DOUBLE NULL COMMENT '纬度',
  `longitude` DOUBLE NULL COMMENT '经度',
  `rating` DOUBLE NULL COMMENT '评分/星级',
  `address` VARCHAR(500) NULL COMMENT '地址',
  `main_image` VARCHAR(500) NULL COMMENT '主图URL',
  `created_at` DATETIME NULL COMMENT '创建时间',
  `updated_at` DATETIME NULL COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giata_supplier_hotel` (`supplier_id`, `supplier_code`, `hotel_code`, `giata_id`),
  KEY `idx_giata_supplier` (`supplier_id`, `supplier_code`),
  KEY `idx_giata_hotel` (`hotel_code`),
  KEY `idx_giata_id` (`giata_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-GIATA酒店映射';

-- 8. sync_log
DROP TABLE IF EXISTS `sync_log`;
CREATE TABLE `sync_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `supplier_code` VARCHAR(100) NOT NULL COMMENT '供应商代码',
  `business_type` VARCHAR(50) NOT NULL COMMENT '业务点：countries/cities/hotels/nationality/giata/all',
  `file_name` VARCHAR(300) NULL COMMENT '同步的文件名',
  `start_time` DATETIME NULL COMMENT '开始时间',
  `end_time` DATETIME NULL COMMENT '结束时间',
  `total_count` BIGINT NULL COMMENT '解析总行数',
  `success_count` BIGINT NULL COMMENT '成功入库数',
  `skip_count` BIGINT NULL COMMENT '跳过行数（主键缺失等）',
  `error_count` BIGINT NULL COMMENT '错误行数',
  `is_success` TINYINT NULL COMMENT '是否成功：1-成功，0-失败',
  `error_message` TEXT NULL COMMENT '错误信息',
  `created_at` DATETIME NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sync_supplier` (`supplier_id`, `supplier_code`),
  KEY `idx_sync_business` (`business_type`),
  KEY `idx_sync_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据同步日志';

SET FOREIGN_KEY_CHECKS = 1;
