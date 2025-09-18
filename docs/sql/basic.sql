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

 Date: 15/09/2025 13:45:42
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for api_call_log
-- ----------------------------
DROP TABLE IF EXISTS `api_call_log`;
CREATE TABLE `api_call_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'API调用日志ID，主键自增',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID，关联supplier_config表',
  `trace_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '链路追踪ID，用于分布式追踪',
  `api_endpoint` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API端点路径',
  `http_method` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HTTP请求方法：GET、POST、PUT、DELETE等',
  `request_headers` json DEFAULT NULL COMMENT '请求头信息，JSON格式',
  `request_params` json DEFAULT NULL COMMENT '请求参数，JSON格式',
  `request_body` longtext COLLATE utf8mb4_unicode_ci COMMENT '请求体内容',
  `response_headers` json DEFAULT NULL COMMENT '响应头信息，JSON格式',
  `response_body` longtext COLLATE utf8mb4_unicode_ci COMMENT '响应体内容',
  `response_status` int DEFAULT NULL COMMENT 'HTTP响应状态码',
  `response_time_ms` bigint DEFAULT NULL COMMENT '响应时间，单位毫秒',
  `error_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误代码',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息详情',
  `retry_count` bigint DEFAULT '0' COMMENT '重试次数',
  `is_success` tinyint(1) DEFAULT '0' COMMENT '是否成功：1-成功，0-失败',
  `business_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务类型：search、booking、cancel等',
  `channel` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调用渠道：API、WEB、MOBILE等',
  `client_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端IP地址，支持IPv6',
  `user_agent` text COLLATE utf8mb4_unicode_ci COMMENT '用户代理信息',
  `app_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用ID标识',
  `user_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户ID',
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话ID',
  `request_size_bytes` bigint DEFAULT NULL COMMENT '请求大小，单位字节',
  `response_size_bytes` bigint DEFAULT NULL COMMENT '响应大小，单位字节',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_supplier_id` (`supplier_id`) COMMENT '供应商ID索引',
  KEY `idx_trace_id` (`trace_id`) COMMENT '链路追踪ID索引',
  KEY `idx_api_endpoint` (`api_endpoint`) COMMENT 'API端点索引',
  KEY `idx_http_method` (`http_method`) COMMENT 'HTTP方法索引',
  KEY `idx_response_status` (`response_status`) COMMENT '响应状态码索引',
  KEY `idx_response_time_ms` (`response_time_ms`) COMMENT '响应时间索引',
  KEY `idx_is_success` (`is_success`) COMMENT '成功状态索引',
  KEY `idx_business_type` (`business_type`) COMMENT '业务类型索引',
  KEY `idx_channel` (`channel`) COMMENT '调用渠道索引',
  KEY `idx_app_id` (`app_id`) COMMENT '应用ID索引',
  KEY `idx_user_id` (`user_id`) COMMENT '用户ID索引',
  KEY `idx_created_at` (`created_at`) COMMENT '创建时间索引',
  KEY `idx_supplier_endpoint_time` (`supplier_id`,`api_endpoint`,`created_at`) COMMENT '供应商端点时间复合索引',
  KEY `idx_status_time` (`response_status`,`created_at`) COMMENT '状态时间复合索引',
  KEY `idx_api_log_trace_id` (`trace_id`),
  KEY `idx_api_log_business_success` (`business_type`,`is_success`),
  CONSTRAINT `api_call_log_ibfk_1` FOREIGN KEY (`supplier_id`) REFERENCES `supplier_config` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API调用日志表';

-- ----------------------------
-- Records of api_call_log
-- ----------------------------
BEGIN;
INSERT INTO `api_call_log` (`id`, `supplier_id`, `trace_id`, `api_endpoint`, `http_method`, `request_headers`, `request_params`, `request_body`, `response_headers`, `response_body`, `response_status`, `response_time_ms`, `error_code`, `error_message`, `retry_count`, `is_success`, `business_type`, `channel`, `client_ip`, `user_agent`, `app_id`, `user_id`, `session_id`, `request_size_bytes`, `response_size_bytes`, `created_at`) VALUES (39, 1, NULL, '/ws/index.php?sel_city=71649&sel_currency=USD&availableonly=1&country_of_residence=1&sel_country=138&roomDetails=[{\"numberOfAdults\":2}]&gzip=no&limit_hotel_room_type=5&chk_ratings=1.0,2.0,3.0,4.0,5.0&checkout_date=16/12/2025&timeout=60&password=Welcome@@123&checkin_date=15/12/2025&hotel_ids=OT000016097&action=hotel_search&sel_nationality=1&static_data=1&username=Heytrip_Test&number_of_rooms=1', 'GET', '{\"User-Agent\": [\"HeyTrip-AsianOverland-Adapter-Pax/1.0\"], \"X-Supplier\": [\"AsianOverland\"], \"Content-Type\": [\"application/json\"]}', '{\"gzip\": \"no\", \"action\": \"hotel_search\", \"timeout\": \"60\", \"password\": \"Welcome@@123\", \"sel_city\": \"71649\", \"username\": \"Heytrip_Test\", \"hotel_ids\": \"OT000016097\", \"chk_ratings\": \"1.0,2.0,3.0,4.0,5.0\", \"roomDetails\": \"[{\\\"numberOfAdults\\\":2}]\", \"sel_country\": \"138\", \"static_data\": \"1\", \"checkin_date\": \"15/12/2025\", \"sel_currency\": \"USD\", \"availableonly\": \"1\", \"checkout_date\": \"16/12/2025\", \"number_of_rooms\": \"1\", \"sel_nationality\": \"1\", \"country_of_residence\": \"1\", \"limit_hotel_room_type\": \"5\"}', '', '{\"Date\": [\"Fri, 12 Sep 2025 05:09:42 GMT\"], \"Vary\": [\"Accept-Encoding\"], \"Content-Type\": [\"application/json\"], \"transfer-encoding\": [\"chunked\"]}', '{\"errorMessage\":\" Your account is not activated yet. Please contact administrator.\",\"success\":false,\"failed\":true,\"Status\":null,\"TotalCount\":0,\"WebServiceVersion\":\"2.0\",\"Message\":\"fail\",\"StartTime\":\"2025-09-12T05:09:42.213094141Z\",\"EndTime\":\"2025-09-12T05:09:42.226027453Z\",\"SearchUniqueId\":\"824-010-20250912050942-010-983753-010-1757653782224299559-010-\",\"MessageInfo\":\" Your account is not activated yet. Please contact administrator.\",\"Login\":null,\"HotelList\":null}', 200, 545, NULL, NULL, 0, 1, 'api_call', 'HTTP_CLIENT', NULL, 'HeyTrip-AsianOverland-Adapter-Pax/1.0', NULL, NULL, NULL, 0, 468, '2025-09-12 13:09:42');
INSERT INTO `api_call_log` (`id`, `supplier_id`, `trace_id`, `api_endpoint`, `http_method`, `request_headers`, `request_params`, `request_body`, `response_headers`, `response_body`, `response_status`, `response_time_ms`, `error_code`, `error_message`, `retry_count`, `is_success`, `business_type`, `channel`, `client_ip`, `user_agent`, `app_id`, `user_id`, `session_id`, `request_size_bytes`, `response_size_bytes`, `created_at`) VALUES (40, 1, NULL, '/ws/index.php?sel_city=71649&sel_currency=USD&availableonly=1&country_of_residence=1&sel_country=138&roomDetails=[{\"numberOfAdults\":2}]&gzip=no&limit_hotel_room_type=5&chk_ratings=1.0,2.0,3.0,4.0,5.0&checkout_date=16/12/2025&timeout=60&password=Welcome@@123&checkin_date=15/12/2025&hotel_ids=OT000016097&action=hotel_search&sel_nationality=1&static_data=1&username=Heytrip_Test&number_of_rooms=1', 'GET', '{\"User-Agent\": [\"HeyTrip-AsianOverland-Adapter-Pax/1.0\"], \"X-Supplier\": [\"AsianOverland\"], \"Content-Type\": [\"application/json\"]}', '{\"gzip\": \"no\", \"action\": \"hotel_search\", \"timeout\": \"60\", \"password\": \"Welcome@@123\", \"sel_city\": \"71649\", \"username\": \"Heytrip_Test\", \"hotel_ids\": \"OT000016097\", \"chk_ratings\": \"1.0,2.0,3.0,4.0,5.0\", \"roomDetails\": \"[{\\\"numberOfAdults\\\":2}]\", \"sel_country\": \"138\", \"static_data\": \"1\", \"checkin_date\": \"15/12/2025\", \"sel_currency\": \"USD\", \"availableonly\": \"1\", \"checkout_date\": \"16/12/2025\", \"number_of_rooms\": \"1\", \"sel_nationality\": \"1\", \"country_of_residence\": \"1\", \"limit_hotel_room_type\": \"5\"}', '', '{\"Date\": [\"Fri, 12 Sep 2025 06:32:46 GMT\"], \"Vary\": [\"Accept-Encoding\"], \"Content-Type\": [\"application/json\"], \"transfer-encoding\": [\"chunked\"]}', '{\"errorMessage\":null,\"success\":true,\"failed\":false,\"Status\":null,\"TotalCount\":1,\"WebServiceVersion\":\"2.0\",\"Message\":\"success\",\"StartTime\":\"2025-09-12T06:32:41.04645224Z\",\"EndTime\":\"2025-09-12T06:32:46.882581776Z\",\"SearchUniqueId\":\"824-010-20250912063241-010-983967-010-1757658761056613853-010-\",\"MessageInfo\":null,\"Login\":null,\"HotelList\":[{\"HotelId\":\"OT000016097\",\"LocalHotelId\":\"OT000016097\",\"HotelName\":\"LE MÉRIDIEN DUBAI HOTEL & CONFERENCE CENTRE\",\"PropertyRating\":\"5.0\",\"Available\":\"1\",\"Latitude\":\"25.249048\",\"Longitude\":\"55.34764\",\"Address\":\"Airport Road PO BOX 10001\",\"ThumbNailUrl\":\"http://colosseum.otrams.com/cpfv3/images/?image=aHR0cHM6Ly9pLnRyYXZlbGFwaS5jb20vaG90ZWxzLzEwMDAwMDAvMTgwMDAwLzE3ODAwMC8xNzc5MzIvMDRhZjUwZmZfYi5qcGc=\",\"RateCurrencyCode\":\"USD\",\"TotalCharges\":157.09,\"HotelProperty\":[{\"DisplayRoomRate\":157.09,\"Type\":\"Selection\",\"SectionUniqueId\":\"ZG90dzoxXzMwOTU0X2RvdWJsZV8xXzJfMF9kZWx1eGVfcm9vbW9ubHlfdHJ1ZV84NkRfODYuMDAlQ1A6cXFxOmdv\",\"RoomRates\":[{\"Available\":1,\"NumberOfRooms\":1,\"NumberOfAdults\":2,\"NumberOfChild\":\"0\",\"ChildAges\":null,\"RoomRate\":157.09,\"RoomType\":\"Deluxe Room Only  - Dynamic Availability \",\"RoomCategory\":\"deluxe\",\"MealBasis\":\"Room Only\",\"MealCode\":\"RO\",\"Note\":\"\",\"ClassUniqueId\":\"MzA5NTRfMF9kb3VibGVfMV8yXzBfZGVsdXhlX3Jvb20gb25seV90cnVl\",\"RateBreakup\":[{\"Date\":\"15-12-2025\",\"Day\":\"Monday\",\"DisplayNightlyRate\":157.09}]}],\"RoomDetails\":[],\"Refundable\":true,\"Policies\":{\"CancellationPolicy\":[{\"Start\":\"2025-12-05 14:00:01 +0530\",\"End\":\"2025-12-15 05:30:00 +0530\",\"Charges\":135.1}]}},{\"DisplayRoomRate\":157.09,\"Type\":\"Selection\",\"SectionUniqueId\":\"ZG90dzoxXzMwOTU0X2RvdWJsZV8xXzJfMF9kZWx1eGVfcm9vbW9ubHlfZmFsc2VfMkRfMTAwLjAwJUNQOnFxcTpnbw==\",\"RoomRates\":[{\"Available\":1,\"NumberOfRooms\":1,\"NumberOfAdults\":2,\"NumberOfChild\":\"0\",\"ChildAges\":null,\"RoomRate\":157.09,\"RoomType\":\"Deluxe Room Only  - Dynamic Availability \",\"RoomCategory\":\"deluxe\",\"MealBasis\":\"Room Only\",\"MealCode\":\"RO\",\"Note\":\"\",\"ClassUniqueId\":\"MzA5NTRfMF9kb3VibGVfMV8yXzBfZGVsdXhlX3Jvb20gb25seV9mYWxzZQ==\",\"RateBreakup\":[{\"Date\":\"15-12-2025\",\"Day\":\"Monday\",\"DisplayNightlyRate\":157.09}]}],\"RoomDetails\":[],\"Refundable\":false,\"Policies\":{\"CancellationPolicy\":[{\"Start\":\"2025-09-10 12:02:46 +0530\",\"End\":\"2025-12-15 05:30:00 +0530\",\"Charges\":157.09}]}},{\"DisplayRoomRate\":158.64,\"Type\":\"Selection\",\"SectionUniqueId\":\"ZG90dzoxXzMwOTU0X2RvdWJsZV8xXzJfMF9zdWl0ZV9yb29tb25seV90cnVlXzg2RF84Ni4wMCVDUDpxcXE6Z28=\",\"RoomRates\":[{\"Available\":1,\"NumberOfRooms\":1,\"NumberOfAdults\":2,\"NumberOfChild\":\"0\",\"ChildAges\":null,\"RoomRate\":158.64,\"RoomType\":\"Suite Room Only  - Dynamic Availability \",\"RoomCategory\":\"SUITE\",\"MealBasis\":\"Room Only\",\"MealCode\":\"RO\",\"Note\":\"\",\"ClassUniqueId\":\"MzA5NTRfMF9kb3VibGVfMV8yXzBfc3VpdGVfcm9vbSBvbmx5X3RydWU=\",\"RateBreakup\":[{\"Date\":\"15-12-2025\",\"Day\":\"Monday\",\"DisplayNightlyRate\":158.64}]}],\"RoomDetails\":[],\"Refundable\":true,\"Policies\":{\"CancellationPolicy\":[{\"Start\":\"2025-12-05 14:00:01 +0530\",\"End\":\"2025-12-15 05:30:00 +0530\",\"Charges\":136.43}]}},{\"DisplayRoomRate\":158.64,\"Type\":\"Selection\",\"SectionUniqueId\":\"ZG90dzoxXzMwOTU0X2RvdWJsZV8xXzJfMF9zdHVkaW9fcm9vbW9ubHlfdHJ1ZV84NkRfODYuMDAlQ1A6cXFxOmdv\",\"RoomRates\":[{\"Available\":1,\"NumberOfRooms\":1,\"NumberOfAdults\":2,\"NumberOfChild\":\"0\",\"ChildAges\":null,\"RoomRate\":158.64,\"RoomType\":\"Studio Room Only  - Dynamic Availability \",\"RoomCategory\":\"STUDIO\",\"MealBasis\":\"Room Only\",\"MealCode\":\"RO\",\"Note\":\"\",\"ClassUniqueId\":\"MzA5NTRfMF9kb3VibGVfMV8yXzBfc3R1ZGlvX3Jvb20gb25seV90cnVl\",\"RateBreakup\":[{\"Date\":\"15-12-2025\",\"Day\":\"Monday\",\"DisplayNightlyRate\":158.64}]}],\"RoomDetails\":[],\"Refundable\":true,\"Policies\":{\"CancellationPolicy\":[{\"Start\":\"2025-12-05 14:00:01 +0530\",\"End\":\"2025-12-15 05:30:00 +0530\",\"Charges\":136.43}]}},{\"DisplayRoomRate\":160.22,\"Type\":\"Selection\",\"SectionUniqueId\":\"ZG90dzoxXzMwOTU0X2RvdWJsZV8xXzJfMF9zdGFuZGFyZF9yb29tb25seV90cnVlXzg2RF84Ni4wMCVDUDpxcXE6Z28=\",\"RoomRates\":[{\"Available\":1,\"NumberOfRooms\":1,\"NumberOfAdults\":2,\"NumberOfChild\":\"0\",\"ChildAges\":null,\"RoomRate\":160.22,\"RoomType\":\"Standard Room Only  - Dynamic Availability \",\"RoomCategory\":\"standard\",\"MealBasis\":\"Room Only\",\"MealCode\":\"RO\",\"Note\":\"\",\"ClassUniqueId\":\"MzA5NTRfMF9kb3VibGVfMV8yXzBfc3RhbmRhcmRfcm9vbSBvbmx5X3RydWU=\",\"RateBreakup\":[{\"Date\":\"15-12-2025\",\"Day\":\"Monday\",\"DisplayNightlyRate\":160.22}]}],\"RoomDetails\":[],\"Refundable\":true,\"Policies\":{\"CancellationPolicy\":[{\"Start\":\"2025-12-05 14:00:01 +0530\",\"End\":\"2025-12-15 05:30:00 +0530\",\"Charges\":137.79}]}}]}]}', 200, 6732, NULL, NULL, 0, 1, 'api_call', 'HTTP_CLIENT', NULL, 'HeyTrip-AsianOverland-Adapter-Pax/1.0', NULL, NULL, NULL, 0, 4437, '2025-09-12 14:32:47');
COMMIT;

-- ----------------------------
-- Table structure for supplier_config
-- ----------------------------
DROP TABLE IF EXISTS `supplier_config`;
CREATE TABLE `supplier_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '供应商配置ID，主键自增',
  `supplier_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商名称，唯一标识',
  `supplier_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商代码，用于系统内部标识',
  `api_base_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商API基础URL地址',
  `auth_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MD5' COMMENT '认证类型：MD5、SHA256、JWT、OAUTH等',
  `auth_config` json DEFAULT NULL COMMENT '认证配置信息，JSON格式存储appId、secretKey等',
  `ftp_config` json DEFAULT NULL COMMENT 'FTP配置信息，JSON格式存储host、port、username、password等',
  `timeout_ms` bigint NOT NULL DEFAULT '30000' COMMENT 'API调用超时时间，单位毫秒',
  `retry_count` int NOT NULL DEFAULT '3' COMMENT 'API调用失败重试次数',
  `max_concurrent_requests` int NOT NULL DEFAULT '10' COMMENT '最大并发请求数',
  `rate_limit_per_second` int NOT NULL DEFAULT '100' COMMENT '每秒请求限制数',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：1-启用，0-禁用',
  `priority` int NOT NULL DEFAULT '100' COMMENT '优先级，数值越小优先级越高',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '供应商描述信息',
  `contact_info` json DEFAULT NULL COMMENT '联系信息，包含邮箱、电话、联系人等',
  `supported_countries` json DEFAULT NULL COMMENT '支持的国家列表，JSON数组格式',
  `supported_cities` json DEFAULT NULL COMMENT '支持的城市列表，JSON数组格式',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT 'system' COMMENT '创建人',
  `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT 'system' COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `supplier_name` (`supplier_name`),
  UNIQUE KEY `supplier_code` (`supplier_code`),
  KEY `idx_supplier_name` (`supplier_name`) COMMENT '供应商名称索引',
  KEY `idx_supplier_code` (`supplier_code`) COMMENT '供应商代码索引',
  KEY `idx_is_active` (`is_active`) COMMENT '启用状态索引',
  KEY `idx_priority` (`priority`) COMMENT '优先级索引',
  KEY `idx_created_at` (`created_at`) COMMENT '创建时间索引'
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商配置表';

-- ----------------------------
-- Records of supplier_config
-- ----------------------------
BEGIN;
INSERT INTO `supplier_config` (`id`, `supplier_name`, `supplier_code`, `api_base_url`, `auth_type`, `auth_config`, `ftp_config`, `timeout_ms`, `retry_count`, `max_concurrent_requests`, `rate_limit_per_second`, `is_active`, `priority`, `description`, `contact_info`, `supported_countries`, `supported_cities`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (1, 'AsianOverland', 'AO_QTECH', 'https://colosseum.otrams.com/ws/index.php', 'BasicAuth', '{\"appId\": \"heytrip_supplier_integration_pax\", \"token\": \"Welcome@@123\", \"password\": \"Welcome@@123\", \"username\": \"Heytrip_Test\", \"secretKey\": \"HeyTrip@Pax#SupplierIntegration!2025\"}', '{\"host\": \"18.170.183.159\", \"port\": 21, \"password\": \"v7QAMfegDWcDBbqx\", \"username\": \"colosseum_live_static_data\", \"citiesPath\": \"ftp,/static_data_cities.csv\", \"hotelsPath\": \"local,classpath:csv/static_data_hotels.csv\", \"countriesPath\": \"ftp,/static_data_countries.csv\", \"giataLocalPath\": \"local,classpath:csv/aosc_giata_id.csv\", \"nationalityPath\": \"ftp,/static_data_nationality.csv\"}', 30000, 3, 10, 50, 1, 10, 'AsianOverland供应商通过QTECH技术通道提供马来西亚酒店资源', '{\"email\": \"godrey.pereira@qtechsoftware.com\", \"phone\": \"+91.22.46050602\", \"contact_person\": \"技术支持团队\"}', '[\"MY\"]', '[\"Kuala Lumpur\", \"Penang\", \"Johor Bahru\", \"Malacca\", \"Ipoh\", \"Kota Kinabalu\", \"Kuching\"]', '2025-09-08 08:33:04', '2025-09-15 02:38:10', 'system', 'system');
INSERT INTO `supplier_config` (`id`, `supplier_name`, `supplier_code`, `api_base_url`, `auth_type`, `auth_config`, `ftp_config`, `timeout_ms`, `retry_count`, `max_concurrent_requests`, `rate_limit_per_second`, `is_active`, `priority`, `description`, `contact_info`, `supported_countries`, `supported_cities`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2, 'TestSupplier', 'TEST_DEMO', 'https://api.testsupplier.com/v1', 'MD5', '{\"appId\": \"test_app_demo\", \"secretKey\": \"demo_secret_key_123\"}', '{\"host\": \"18.170.183.159\", \"port\": 21, \"password\": \"v7QAMfegDWcDBbqx\", \"username\": \"colosseum_live_static_data\", \"citiesPath\": \"/static_data_cities.csv\", \"hotelsPath\": \"/static_data_hotels.csv\", \"countriesPath\": \"/static_data_countries.csv\", \"giataLocalPath\": \"classpath:giata/aosc_giata_id.csv\", \"nationalityPath\": \"/static_data_nationality.csv\"}', 25000, 2, 5, 20, 1, 20, '测试供应商，用于开发和测试环境', '{\"email\": \"test@testsupplier.com\", \"phone\": \"+1-555-0123\", \"contact_person\": \"Test Support\"}', '[\"US\", \"CA\"]', '[\"New York\", \"Los Angeles\", \"Toronto\", \"Vancouver\"]', '2025-09-08 08:33:04', '2025-09-12 15:14:08', 'system', 'system');
COMMIT;

-- ----------------------------
-- Table structure for supplier_health_log
-- ----------------------------
DROP TABLE IF EXISTS `supplier_health_log`;
CREATE TABLE `supplier_health_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '健康检查日志ID，主键自增',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID，关联supplier_config表',
  `check_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ping' COMMENT '检查类型：ping、api_test、full_check等',
  `response_time_ms` bigint DEFAULT NULL COMMENT '响应时间，单位毫秒',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `check_details` json DEFAULT NULL COMMENT '检查详情，JSON格式',
  `health_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'UP' COMMENT '健康状态：''UP'',''DOWN'',''DEGRADED'',''UNKNOWN''',
  `status_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态消息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检查时间',
  PRIMARY KEY (`id`),
  KEY `idx_supplier_id` (`supplier_id`) COMMENT '供应商ID索引',
  KEY `idx_check_type` (`check_type`) COMMENT '检查类型索引',
  KEY `idx_health_status` (`health_status`) COMMENT '健康状态枚举索引',
  KEY `idx_created_at` (`created_at`) COMMENT '检查时间索引',
  KEY `idx_supplier_status_time` (`supplier_id`,`created_at`) COMMENT '供应商状态时间复合索引',
  KEY `idx_supplier_health_status_time` (`supplier_id`,`health_status`,`created_at`) COMMENT '供应商健康状态时间复合索引',
  CONSTRAINT `supplier_health_log_ibfk_1` FOREIGN KEY (`supplier_id`) REFERENCES `supplier_config` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商健康检查日志表';

-- ----------------------------
-- Records of supplier_health_log
-- ----------------------------
BEGIN;
INSERT INTO `supplier_health_log` (`id`, `supplier_id`, `check_type`, `response_time_ms`, `error_message`, `check_details`, `health_status`, `status_message`, `created_at`) VALUES (1, 1, 'ping', 150, NULL, '{\"method\": \"GET\", \"endpoint\": \"/health\", \"timestamp\": \"2025-09-08T15:30:00Z\"}', 'UP', NULL, '2025-09-08 08:33:04');
INSERT INTO `supplier_health_log` (`id`, `supplier_id`, `check_type`, `response_time_ms`, `error_message`, `check_details`, `health_status`, `status_message`, `created_at`) VALUES (2, 1, 'api_test', 800, NULL, '{\"timestamp\": \"2025-09-08T15:30:30Z\", \"test_endpoint\": \"/hotel/search\", \"sample_request_success\": true}', 'UP', NULL, '2025-09-08 08:33:04');
INSERT INTO `supplier_health_log` (`id`, `supplier_id`, `check_type`, `response_time_ms`, `error_message`, `check_details`, `health_status`, `status_message`, `created_at`) VALUES (3, 2, 'ping', NULL, 'Connection timeout', '{\"error\": \"timeout after 5000ms\", \"method\": \"GET\", \"endpoint\": \"/health\", \"timestamp\": \"2025-09-08T15:31:00Z\"}', 'DOWN', NULL, '2025-09-08 08:33:04');
INSERT INTO `supplier_health_log` (`id`, `supplier_id`, `check_type`, `response_time_ms`, `error_message`, `check_details`, `health_status`, `status_message`, `created_at`) VALUES (4, 2, 'api_test', NULL, 'Service unavailable', '{\"error\": \"503 Service Unavailable\", \"timestamp\": \"2025-09-08T15:31:30Z\", \"test_endpoint\": \"/api/v1/status\"}', 'DOWN', NULL, '2025-09-08 08:33:04');
COMMIT;

-- ----------------------------
-- Table structure for sync_log
-- ----------------------------
DROP TABLE IF EXISTS `sync_log`;
CREATE TABLE `sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '供应商代码',
  `business_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务点：countries/cities/hotels/nationality/giata/all',
  `file_name` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '同步的文件名',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `total_count` bigint DEFAULT NULL COMMENT '解析总行数',
  `success_count` bigint DEFAULT NULL COMMENT '成功入库数',
  `skip_count` bigint DEFAULT NULL COMMENT '跳过行数（主键缺失等）',
  `error_count` bigint DEFAULT NULL COMMENT '错误行数',
  `is_success` tinyint DEFAULT NULL COMMENT '是否成功：1-成功，0-失败',
  `message` text COLLATE utf8mb4_general_ci COMMENT '同步消息日志（包含成功和错误的日志）',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sync_supplier` (`supplier_id`,`supplier_code`),
  KEY `idx_sync_business` (`business_type`),
  KEY `idx_sync_created` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='静态数据同步日志';

-- ----------------------------
-- Records of sync_log
-- ----------------------------
BEGIN;
INSERT INTO `sync_log` (`id`, `supplier_id`, `supplier_code`, `business_type`, `file_name`, `start_time`, `end_time`, `total_count`, `success_count`, `skip_count`, `error_count`, `is_success`, `message`, `created_at`) VALUES (30, 1, 'AO_QTECH', 'countries', 'ftp,/static_data_countries.csv', '2025-09-15 11:15:35', '2025-09-15 11:15:49', 0, 0, 0, 0, 1, NULL, '2025-09-15 11:15:49');
INSERT INTO `sync_log` (`id`, `supplier_id`, `supplier_code`, `business_type`, `file_name`, `start_time`, `end_time`, `total_count`, `success_count`, `skip_count`, `error_count`, `is_success`, `message`, `created_at`) VALUES (31, 1, 'AO_QTECH', 'cities', 'ftp,/static_data_cities.csv', '2025-09-15 11:15:49', '2025-09-15 11:16:41', 0, 0, 0, 0, 1, NULL, '2025-09-15 11:16:41');
INSERT INTO `sync_log` (`id`, `supplier_id`, `supplier_code`, `business_type`, `file_name`, `start_time`, `end_time`, `total_count`, `success_count`, `skip_count`, `error_count`, `is_success`, `message`, `created_at`) VALUES (32, 1, 'AO_QTECH', 'hotels', 'local,classpath:csv/static_data_hotels.csv', '2025-09-15 11:16:41', '2025-09-15 11:20:38', 0, 0, 0, 0, 1, NULL, '2025-09-15 11:20:38');
INSERT INTO `sync_log` (`id`, `supplier_id`, `supplier_code`, `business_type`, `file_name`, `start_time`, `end_time`, `total_count`, `success_count`, `skip_count`, `error_count`, `is_success`, `message`, `created_at`) VALUES (33, 1, 'AO_QTECH', 'nationality', 'ftp,/static_data_nationality.csv', '2025-09-15 11:20:38', '2025-09-15 11:20:51', 0, 0, 0, 0, 1, NULL, '2025-09-15 11:20:51');
INSERT INTO `sync_log` (`id`, `supplier_id`, `supplier_code`, `business_type`, `file_name`, `start_time`, `end_time`, `total_count`, `success_count`, `skip_count`, `error_count`, `is_success`, `message`, `created_at`) VALUES (34, 1, 'AO_QTECH', 'giata', 'local,classpath:csv/aosc_giata_id.csv', '2025-09-15 11:20:51', '2025-09-15 11:20:51', 0, 0, 0, 0, 1, NULL, '2025-09-15 11:20:51');
INSERT INTO `sync_log` (`id`, `supplier_id`, `supplier_code`, `business_type`, `file_name`, `start_time`, `end_time`, `total_count`, `success_count`, `skip_count`, `error_count`, `is_success`, `message`, `created_at`) VALUES (35, 1, 'AO_QTECH', 'countries', 'ftp,/static_data_countries.csv', '2025-09-15 11:24:50', '2025-09-15 11:25:02', 0, 0, 0, 0, 1, NULL, '2025-09-15 11:25:02');
INSERT INTO `sync_log` (`id`, `supplier_id`, `supplier_code`, `business_type`, `file_name`, `start_time`, `end_time`, `total_count`, `success_count`, `skip_count`, `error_count`, `is_success`, `message`, `created_at`) VALUES (36, 1, 'AO_QTECH', 'countries', 'ftp,/static_data_countries.csv', '2025-09-15 11:28:43', '2025-09-15 11:28:56', 0, 0, 0, 0, 1, NULL, '2025-09-15 11:28:56');
INSERT INTO `sync_log` (`id`, `supplier_id`, `supplier_code`, `business_type`, `file_name`, `start_time`, `end_time`, `total_count`, `success_count`, `skip_count`, `error_count`, `is_success`, `message`, `created_at`) VALUES (37, 1, 'AO_QTECH', 'countries', 'ftp,/static_data_countries.csv', '2025-09-15 11:38:42', '2025-09-15 11:38:55', 0, 0, 0, 0, 1, NULL, '2025-09-15 11:38:55');
INSERT INTO `sync_log` (`id`, `supplier_id`, `supplier_code`, `business_type`, `file_name`, `start_time`, `end_time`, `total_count`, `success_count`, `skip_count`, `error_count`, `is_success`, `message`, `created_at`) VALUES (38, 1, 'AO_QTECH', 'countries', 'ftp,/static_data_countries.csv', '2025-09-15 11:57:30', '2025-09-15 11:57:42', 250, 250, 0, 0, 1, NULL, '2025-09-15 11:57:42');
INSERT INTO `sync_log` (`id`, `supplier_id`, `supplier_code`, `business_type`, `file_name`, `start_time`, `end_time`, `total_count`, `success_count`, `skip_count`, `error_count`, `is_success`, `message`, `created_at`) VALUES (39, 1, 'AO_QTECH', 'cities', 'ftp,/static_data_cities.csv', '2025-09-15 11:57:42', '2025-09-15 11:58:19', 38265, 38265, 0, 0, 1, NULL, '2025-09-15 11:58:19');
INSERT INTO `sync_log` (`id`, `supplier_id`, `supplier_code`, `business_type`, `file_name`, `start_time`, `end_time`, `total_count`, `success_count`, `skip_count`, `error_count`, `is_success`, `message`, `created_at`) VALUES (40, 1, 'AO_QTECH', 'nationality', 'ftp,/static_data_nationality.csv', '2025-09-15 11:58:19', '2025-09-15 11:58:32', 250, 250, 0, 0, 1, NULL, '2025-09-15 11:58:32');
COMMIT;

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '系统配置ID，主键自增',
  `config_key` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键名，唯一标识',
  `config_value` text COLLATE utf8mb4_unicode_ci COMMENT '配置值',
  `config_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STRING' COMMENT '配置类型：STRING、NUMBER、BOOLEAN、JSON等',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '配置描述',
  `is_encrypted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否加密存储：1-是，0-否',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：1-启用，0-禁用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT 'system' COMMENT '创建人',
  `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT 'system' COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`),
  KEY `idx_config_key` (`config_key`) COMMENT '配置键名索引',
  KEY `idx_config_type` (`config_type`) COMMENT '配置类型索引',
  KEY `idx_is_active` (`is_active`) COMMENT '启用状态索引'
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ----------------------------
-- Records of system_config
-- ----------------------------
BEGIN;
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `config_type`, `description`, `is_encrypted`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (1, 'system.default.timeout_ms', '30000', 'NUMBER', '系统默认API调用超时时间（毫秒）', 0, 1, '2025-09-08 08:33:04', '2025-09-08 08:33:04', 'system', 'system');
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `config_type`, `description`, `is_encrypted`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2, 'system.default.retry_count', '3', 'NUMBER', '系统默认重试次数', 0, 1, '2025-09-08 08:33:04', '2025-09-08 08:33:04', 'system', 'system');
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `config_type`, `description`, `is_encrypted`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (3, 'system.max_concurrent_requests', '100', 'NUMBER', '系统最大并发请求数', 0, 1, '2025-09-08 08:33:04', '2025-09-08 08:33:04', 'system', 'system');
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `config_type`, `description`, `is_encrypted`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (4, 'system.health_check.interval_seconds', '60', 'NUMBER', '健康检查间隔时间（秒）', 0, 1, '2025-09-08 08:33:04', '2025-09-08 08:33:04', 'system', 'system');
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `config_type`, `description`, `is_encrypted`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (5, 'system.log.retention_days', '30', 'NUMBER', 'API调用日志保留天数', 0, 1, '2025-09-08 08:33:04', '2025-09-08 08:33:04', 'system', 'system');
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `config_type`, `description`, `is_encrypted`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (6, 'system.cache.enabled', 'true', 'BOOLEAN', '是否启用缓存', 0, 1, '2025-09-08 08:33:04', '2025-09-08 08:33:04', 'system', 'system');
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `config_type`, `description`, `is_encrypted`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (7, 'system.cache.ttl_seconds', '300', 'NUMBER', '缓存过期时间（秒）', 0, 1, '2025-09-08 08:33:04', '2025-09-08 08:33:04', 'system', 'system');
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `config_type`, `description`, `is_encrypted`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (8, 'system.security.encryption_key', 'heytrip_secret_key_2025', 'STRING', '系统加密密钥', 1, 1, '2025-09-08 08:33:04', '2025-09-08 08:33:04', 'system', 'system');
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `config_type`, `description`, `is_encrypted`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (9, 'system.notification.email.enabled', 'false', 'BOOLEAN', '是否启用邮件通知', 0, 1, '2025-09-08 08:33:04', '2025-09-08 08:33:04', 'system', 'system');
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `config_type`, `description`, `is_encrypted`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (10, 'system.notification.webhook.url', '', 'STRING', 'Webhook通知地址', 0, 0, '2025-09-08 08:33:04', '2025-09-08 08:33:04', 'system', 'system');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
