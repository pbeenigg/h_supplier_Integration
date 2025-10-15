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

 Date: 15/10/2025 11:43:13
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for distribution_call_log
-- ----------------------------
DROP TABLE IF EXISTS `distribution_call_log`;
CREATE TABLE `distribution_call_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'API调用日志ID，主键自增',
  `app_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用ID标识',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID，关联supplier_config表',
  `trace_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '链路追踪ID，用于追踪请求链路',
  `api_endpoint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API端点路径',
  `http_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HTTP请求方法：GET、POST、PUT、DELETE等',
  `request_headers` json DEFAULT NULL COMMENT '请求头信息，JSON格式',
  `request_params` json DEFAULT NULL COMMENT '请求参数，JSON格式',
  `request_body` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '请求体内容',
  `request_body_compressed` tinyint(1) DEFAULT '0' COMMENT '请求体是否压缩：1-已压缩，0-未压缩',
  `response_headers` json DEFAULT NULL COMMENT '响应头信息，JSON格式',
  `response_body` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '响应体内容',
  `response_body_compressed` tinyint(1) DEFAULT '0' COMMENT '响应体是否压缩：1-已压缩，0-未压缩',
  `response_status` int DEFAULT NULL COMMENT 'HTTP响应状态码',
  `response_time_ms` bigint DEFAULT NULL COMMENT '响应时间，单位毫秒',
  `error_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误代码',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错误信息详情',
  `is_success` tinyint(1) DEFAULT '0' COMMENT '是否成功：1-成功，0-失败',
  `business_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务类型：queryOrder、createOrder、cancelOrder、getPrice、getPrices、getHotel、orderCheck、modifyOrder等',
  `hotel_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '酒店标识',
  `check_in_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入住标识',
  `check_out_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '离店标识',
  `distribution_orders_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分销商订单号标识',
  `client_ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端IP地址，支持IPv6',
  `user_agent` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用户代理信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_supplier_id` (`supplier_id`) COMMENT '供应商ID索引',
  KEY `idx_is_success` (`is_success`) COMMENT '成功状态索引',
  KEY `idx_business_type` (`business_type`) COMMENT '业务类型索引',
  KEY `idx_trace_id` (`trace_id`) COMMENT '追踪ID索引',
  KEY `idx_created_at` (`created_at`) COMMENT '创建时间索引',
  KEY `idx_distribution_orders_key` (`distribution_orders_key`) COMMENT '分销商订单号索引'
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分销商调用日志表';

-- ----------------------------
-- Table structure for distribution_orders_log
-- ----------------------------
DROP TABLE IF EXISTS `distribution_orders_log`;
CREATE TABLE `distribution_orders_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'API调用日志ID，主键自增',
  `app_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用ID标识',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID，关联supplier_config表',
  `trace_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '链路追踪ID，用于追踪请求链路',
  `hotel_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '酒店标识',
  `check_in_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入住标识',
  `check_out_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '离店标识',
  `room_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '房型标识',
  `rate_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '房价标识',
  `nights` int DEFAULT NULL COMMENT '入住晚数',
  `guests` int DEFAULT NULL COMMENT '入住人数',
  `rooms` int DEFAULT NULL COMMENT '预订房间数',
  `occupancy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入住人信息 2-5-3代表2成人2个儿童（1个5岁，1个3岁） 多间房下滑线_分割',
  `currency` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '货币代码，如CNY、USD等',
  `national` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国家代码，如CN、US等',
  `total_amount` decimal(10,2) DEFAULT NULL COMMENT '总金额',
  `distribution_orders_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分销商订单号标识',
  `supplier_booking_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商预定标识',
  `booking_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预定状态',
  `error_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误代码',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错误信息详情',
  `is_success` tinyint(1) DEFAULT '0' COMMENT '是否成功：1-成功，0-失败',
  `business_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务类型：createOrder、cancelOrder、orderCheck、modifyOrde等',
  `orginal_request` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '原始请求内容',
  `response_body_compressed` tinyint(1) DEFAULT '0' COMMENT '响应体是否压缩：1-已压缩，0-未压缩',
  `request_body_compressed` tinyint(1) DEFAULT '0' COMMENT '请求体是否压缩：1-已压缩，0-未压缩',
  `orginal_response` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '原始响应内容',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_supplier_id` (`supplier_id`) COMMENT '供应商ID索引',
  KEY `idx_is_success` (`is_success`) COMMENT '成功状态索引',
  KEY `idx_business_type` (`business_type`) COMMENT '业务类型索引',
  KEY `idx_trace_id` (`trace_id`) COMMENT '追踪ID索引',
  KEY `idx_distribution_orders_key` (`distribution_orders_key`) COMMENT '分销商订单号索引',
  KEY `idx_created_at` (`created_at`) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分销商订单日志表';

SET FOREIGN_KEY_CHECKS = 1;
