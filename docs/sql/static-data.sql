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

 Date: 15/09/2025 13:44:09
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
) ENGINE=InnoDB AUTO_INCREMENT=229592 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-城市';

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
) ENGINE=InnoDB AUTO_INCREMENT=2252 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-国家';

-- ----------------------------
-- Table structure for hotel
-- ----------------------------
DROP TABLE IF EXISTS `hotel`;
CREATE TABLE `hotel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '供应商代码',
  `hotel_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '酒店编码/ID',
  `name` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '酒店名称',
  `city_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '城市代码',
  `city_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '城市名称',
  `country_code` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '国家代码',
  `main_image` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主图URL',
  `short_desc` text COLLATE utf8mb4_general_ci COMMENT '简述',
  `long_desc` longtext COLLATE utf8mb4_general_ci COMMENT '描述',
  `address` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地址',
  `phone` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '电话',
  `website` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '官网',
  `latitude` double DEFAULT NULL COMMENT '纬度',
  `longitude` double DEFAULT NULL COMMENT '经度',
  `rating` double DEFAULT NULL COMMENT '评分/星级',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hotel_supplier_code` (`supplier_id`,`supplier_code`,`hotel_code`,`id`) USING BTREE,
  KEY `idx_hotel_supplier` (`supplier_id`,`supplier_code`),
  KEY `idx_hotel_code` (`hotel_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2091143 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-酒店';

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
) ENGINE=InnoDB AUTO_INCREMENT=3034 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-GIATA酒店映射';

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
) ENGINE=InnoDB AUTO_INCREMENT=1251 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-国籍';

-- ----------------------------
-- Table structure for rate_plan
-- ----------------------------
DROP TABLE IF EXISTS `rate_plan`;
CREATE TABLE `rate_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '供应商代码',
  `rate_plan_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '价格计划编码',
  `hotel_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属酒店编码',
  `room_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属房型编码',
  `name` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '价格计划名称',
  `meal` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '餐食类型',
  `cancellation_policy` text COLLATE utf8mb4_general_ci COMMENT '取消政策',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rateplan_supplier_code` (`supplier_id`,`supplier_code`,`rate_plan_code`),
  KEY `idx_rateplan_supplier` (`supplier_id`,`supplier_code`),
  KEY `idx_rateplan_code` (`rate_plan_code`),
  KEY `idx_rateplan_hotel` (`hotel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-价格计划';

-- ----------------------------
-- Table structure for room
-- ----------------------------
DROP TABLE IF EXISTS `room`;
CREATE TABLE `room` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '供应商代码',
  `room_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '房型编码',
  `hotel_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属酒店编码',
  `name` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '房型名称',
  `bed_type` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '床型',
  `area` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '面积',
  `occupancy` int DEFAULT NULL COMMENT '最大入住人数',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_supplier_code` (`supplier_id`,`supplier_code`,`room_code`),
  KEY `idx_room_supplier` (`supplier_id`,`supplier_code`),
  KEY `idx_room_code` (`room_code`),
  KEY `idx_room_hotel` (`hotel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据-房型';

SET FOREIGN_KEY_CHECKS = 1;
