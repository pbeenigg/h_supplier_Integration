/*
 Navicat Premium Dump SQL

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 80032 (8.0.32)
 Source Host           : 127.0.0.1:3306
 Source Schema         : supplier_pax

 Target Server Type    : MySQL
 Target Server Version : 80032 (8.0.32)
 File Encoding         : 65001

 Date: 18/09/2025 18:12:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for city
-- ----------------------------
DROP TABLE IF EXISTS `city`;
CREATE TABLE `city` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '供应商代码',
  `city_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '城市代码',
  `name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '城市名称',
  `country_code` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '国家代码',
  `country_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '国家名称',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_city_supplier_code` (`supplier_id`,`supplier_code`,`city_code`,`id`) USING BTREE,
  KEY `idx_city_supplier` (`supplier_id`,`supplier_code`),
  KEY `idx_city_code` (`city_code`)
) ENGINE=InnoDB AUTO_INCREMENT=153061 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-城市';

-- ----------------------------
-- Table structure for country
-- ----------------------------
DROP TABLE IF EXISTS `country`;
CREATE TABLE `country` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '供应商代码',
  `country_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '国家代码',
  `country_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '国家名称',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_country_supplier_code` (`supplier_id`,`supplier_code`,`country_code`,`id`) USING BTREE,
  KEY `idx_country_supplier` (`supplier_id`,`supplier_code`),
  KEY `idx_country_code` (`country_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-国家';

-- ----------------------------
-- Table structure for hotel
-- ----------------------------
DROP TABLE IF EXISTS `hotel`;
CREATE TABLE `hotel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商代码',
  `hotel_code` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '酒店编号',
  `hotel_code_md5` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '酒店编码Md5',
  `name` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '酒店名称',
  `locale_name` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '酒店本地化名称(国际)',
  `country_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国家代码',
  `country_id` int DEFAULT NULL COMMENT '国家编号',
  `country` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国家',
  `city_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '城市代码',
  `city_id` int DEFAULT NULL COMMENT '城市编号',
  `city` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '城市',
  `area_id` int DEFAULT NULL COMMENT '所属区域/附近区域编号',
  `area` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属区域/附近区域',
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地址',
  `address_locale` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地址本地化名称(国际)',
  `phone` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电话',
  `latitude` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '纬度',
  `longitude` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '经度',
  `rating` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '星级',
  `hotel_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '酒店类型',
  `brand` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品牌',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '简介/描述',
  `long_desc` longtext COLLATE utf8mb4_unicode_ci COMMENT '详细描述',
  `min_price` decimal(10,2) DEFAULT NULL COMMENT '最低价格',
  `status` int DEFAULT '1' COMMENT '状态：1在线，0下线，-1黑名单',
  `number_of_rooms` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客房总数',
  `year_property_opened` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '建成年份',
  `most_recent_renovation` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最近装修年份',
  `check_in_from` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入住办理起始时间',
  `check_out_util` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退房办理截止时间',
  `postal_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮编',
  `hero_img` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '首图（中等尺寸）',
  `email` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `website` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '官网',
  `ext` text COLLATE utf8mb4_unicode_ci COMMENT '扩展信息',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `sync_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '同步时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
  `is_bookable` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否可预定：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hotel_supplier_code` (`supplier_id`,`supplier_code`,`hotel_code`,`city_code`) USING BTREE,
  KEY `idx_hotel_supplier` (`supplier_id`,`supplier_code`),
  KEY `idx_hotel_code` (`hotel_code`),
  KEY `idx_hotel_city` (`city_code`),
  KEY `idx_hotel_country` (`country_code`),
  KEY `idx_hotel_code_md5` (`hotel_code_md5`),
  KEY `idx_supplier_id_supplier_code_is_bookable` (`supplier_id`,`supplier_code`,`is_bookable`)
) ENGINE=InnoDB AUTO_INCREMENT=2081812 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='静态数据-酒店';

-- ----------------------------
-- Table structure for hotel_bookable
-- ----------------------------
DROP TABLE IF EXISTS `hotel_bookable`;
CREATE TABLE `hotel_bookable` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商代码',
  `hotel_code` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '酒店代码',
  `hotel_code_md5` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '酒店代码MD5',
  `hotel_id` bigint DEFAULT NULL COMMENT '酒店表主键ID（关联Hotel表）',
  `name` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '酒店名称',
  `min_price` decimal(10,2) DEFAULT NULL COMMENT '最低价格',
  `is_bookable` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否可预定',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hotel_bookable_supplier_code` (`supplier_id`,`supplier_code`,`hotel_code`,`hotel_id`) USING BTREE,
  KEY `idx_hotel_bookable_supplier` (`supplier_id`,`supplier_code`),
  KEY `idx_hotel_bookable_code` (`hotel_code`),
  KEY `idx_hotel_bookable_code_md5` (`hotel_code_md5`),
  KEY `idx_hotel_bookable_hotel_id` (`hotel_id`),
  KEY `idx_hotel_bookable_bookable` (`is_bookable`)
) ENGINE=InnoDB AUTO_INCREMENT=8109 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可预定酒店表';

-- ----------------------------
-- Table structure for hotel_giata
-- ----------------------------
DROP TABLE IF EXISTS `hotel_giata`;
CREATE TABLE `hotel_giata` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '供应商代码',
  `hotel_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '酒店编码/ID',
  `giata_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'GIATA 编码',
  `name` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '酒店名称',
  `city_code` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '城市代码',
  `city_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '城市名称',
  `country_code` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '国家代码',
  `long_desc` longtext COLLATE utf8mb4_general_ci COMMENT '描述',
  `latitude` double DEFAULT NULL COMMENT '纬度',
  `longitude` double DEFAULT NULL COMMENT '经度',
  `rating` double DEFAULT NULL COMMENT '评分/星级',
  `address` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地址',
  `main_image` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主图URL',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_giata_supplier_hotel` (`city_code`,`supplier_code`,`hotel_code`,`giata_id`) USING BTREE,
  KEY `idx_giata_supplier` (`supplier_id`,`supplier_code`),
  KEY `idx_giata_hotel` (`hotel_code`),
  KEY `idx_giata_id` (`giata_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3111 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-GIATA酒店映射';

-- ----------------------------
-- Table structure for nationality
-- ----------------------------
DROP TABLE IF EXISTS `nationality`;
CREATE TABLE `nationality` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '供应商代码',
  `nationality_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '国籍代码',
  `nationality` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '国籍名称',
  `iso_code` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ISO代码',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_nat_supplier_code` (`supplier_id`,`supplier_code`,`nationality_code`,`id`) USING BTREE,
  KEY `idx_nat_supplier` (`supplier_id`,`supplier_code`),
  KEY `idx_nat_code` (`nationality_code`)
) ENGINE=InnoDB AUTO_INCREMENT=751 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-国籍';

-- ----------------------------
-- Table structure for rate_plan
-- ----------------------------
DROP TABLE IF EXISTS `rate_plan`;
CREATE TABLE `rate_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商代码',
  `rate_plan_code` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '价格计划编码',
  `rate_plan_code_md5` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '价格计划编码MD5',
  `hotel_id` bigint NOT NULL COMMENT '酒店ID',
  `hotel_code` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '酒店编码',
  `room_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属房型编码',
  `name` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '价格计划名称',
  `meal` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '餐食类型',
  `cancellation_policy` text COLLATE utf8mb4_unicode_ci COMMENT '取消政策',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rateplan_supplier_code` (`supplier_id`,`supplier_code`,`rate_plan_code`),
  KEY `idx_rateplan_supplier` (`supplier_id`,`supplier_code`),
  KEY `idx_rateplan_code` (`rate_plan_code`),
  KEY `idx_rateplan_hotel` (`hotel_code`),
  KEY `idx_rate_plan_code_md5` (`rate_plan_code_md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='静态数据-价格计划';

-- ----------------------------
-- Table structure for room
-- ----------------------------
DROP TABLE IF EXISTS `room`;
CREATE TABLE `room` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商代码',
  `hotel_id` bigint NOT NULL COMMENT '酒店ID',
  `hotel_code` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '酒店编码',
  `room_code` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '房型编码',
  `room_code_md5` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '房型编码MD5',
  `room_name` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物理房型名称',
  `room_name_en` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物理房型名称(英文)',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '描述',
  `room_quantity` int DEFAULT NULL COMMENT '房间数量',
  `ext` text COLLATE utf8mb4_unicode_ci COMMENT '扩展字段',
  `max_occupancy` int DEFAULT NULL COMMENT '最大入住人数',
  `max_occupancy_info` text COLLATE utf8mb4_unicode_ci COMMENT '最大入住人数具体描述(JSON)',
  `bed_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '床型',
  `bed_type_desc` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '床型描述',
  `bed_type_desc_en` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '床型描述(英文)',
  `bed_rooms` text COLLATE utf8mb4_unicode_ci COMMENT '卧室床型明细(JSON)',
  `floor` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '楼层',
  `area` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '面积',
  `views` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '房型景观-国际',
  `bed_width` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '床宽',
  `no_smoking` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '禁烟信息',
  `images` text COLLATE utf8mb4_unicode_ci COMMENT '图片(JSON)',
  `min_price` decimal(10,2) DEFAULT NULL COMMENT '最低价格',
  `min_base_price` decimal(10,2) DEFAULT NULL COMMENT '最低基础价格',
  `facilities` text COLLATE utf8mb4_unicode_ci COMMENT '房型设施(JSON)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_supplier_code` (`supplier_id`,`supplier_code`,`room_code`,`hotel_code`) USING BTREE,
  KEY `idx_room_supplier` (`supplier_id`,`supplier_code`),
  KEY `idx_room_code` (`room_code`),
  KEY `idx_room_hotel` (`hotel_id`),
  KEY `idx_room_code_md5` (`room_code_md5`)
) ENGINE=InnoDB AUTO_INCREMENT=42049 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='静态数据-房型';

SET FOREIGN_KEY_CHECKS = 1;
