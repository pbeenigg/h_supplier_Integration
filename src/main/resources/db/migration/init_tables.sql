-- =====================================================
-- HeyTrip 酒店供应商集成服务数据库表结构
-- 创建时间: 2025-09-08
-- 作者: Pax
-- =====================================================

-- =====================================================
-- 供应商配置表 - 存储供应商基础配置信息
-- =====================================================
CREATE TABLE supplier_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '供应商配置ID，主键自增',
    supplier_name VARCHAR(100) NOT NULL UNIQUE COMMENT '供应商名称，唯一标识',
    supplier_code VARCHAR(50) NOT NULL UNIQUE COMMENT '供应商代码，用于系统内部标识',
    api_base_url VARCHAR(500) NOT NULL COMMENT '供应商API基础URL地址',
    auth_type VARCHAR(50) NOT NULL DEFAULT 'MD5' COMMENT '认证类型：MD5、SHA256、JWT、OAUTH等',
    auth_config JSON COMMENT '认证配置信息，JSON格式存储appId、secretKey等',
    timeout_ms BIGINT NOT NULL DEFAULT 30000 COMMENT 'API调用超时时间，单位毫秒',
    retry_count INT NOT NULL DEFAULT 3 COMMENT 'API调用失败重试次数',
    max_concurrent_requests INT NOT NULL DEFAULT 10 COMMENT '最大并发请求数',
    rate_limit_per_second INT NOT NULL DEFAULT 100 COMMENT '每秒请求限制数',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1-启用，0-禁用',
    priority INT NOT NULL DEFAULT 100 COMMENT '优先级，数值越小优先级越高',
    description TEXT COMMENT '供应商描述信息',
    contact_info JSON COMMENT '联系信息，包含邮箱、电话、联系人等',
    supported_countries JSON COMMENT '支持的国家列表，JSON数组格式',
    supported_cities JSON COMMENT '支持的城市列表，JSON数组格式',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(100) DEFAULT 'system' COMMENT '创建人',
    updated_by VARCHAR(100) DEFAULT 'system' COMMENT '更新人',
    
    -- 索引定义
    INDEX idx_supplier_name (supplier_name) COMMENT '供应商名称索引',
    INDEX idx_supplier_code (supplier_code) COMMENT '供应商代码索引',
    INDEX idx_is_active (is_active) COMMENT '启用状态索引',
    INDEX idx_priority (priority) COMMENT '优先级索引',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商配置表';

-- =====================================================
-- API调用日志表 - 记录所有API调用的详细信息
-- =====================================================
CREATE TABLE api_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'API调用日志ID，主键自增',
    supplier_id BIGINT COMMENT '供应商ID，关联supplier_config表',
    trace_id VARCHAR(100) COMMENT '链路追踪ID，用于分布式追踪',
    api_endpoint VARCHAR(500) NOT NULL COMMENT 'API端点路径',
    http_method VARCHAR(10) NOT NULL COMMENT 'HTTP请求方法：GET、POST、PUT、DELETE等',
    request_headers JSON COMMENT '请求头信息，JSON格式',
    request_params JSON COMMENT '请求参数，JSON格式',
    request_body LONGTEXT COMMENT '请求体内容',
    response_headers JSON COMMENT '响应头信息，JSON格式',
    response_body LONGTEXT COMMENT '响应体内容',
    response_status INT COMMENT 'HTTP响应状态码',
    response_time_ms BIGINT COMMENT '响应时间，单位毫秒',
    error_code VARCHAR(50) COMMENT '错误代码',
    error_message TEXT COMMENT '错误信息详情',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    is_success BOOLEAN DEFAULT FALSE COMMENT '是否成功：1-成功，0-失败',
    business_type VARCHAR(100) COMMENT '业务类型：search、booking、cancel等',
    channel VARCHAR(50) COMMENT '调用渠道：API、WEB、MOBILE等',
    client_ip VARCHAR(45) COMMENT '客户端IP地址，支持IPv6',
    user_agent TEXT COMMENT '用户代理信息',
    app_id VARCHAR(100) COMMENT '应用ID标识',
    user_id VARCHAR(100) COMMENT '用户ID',
    session_id VARCHAR(100) COMMENT '会话ID',
    request_size_bytes BIGINT COMMENT '请求大小，单位字节',
    response_size_bytes BIGINT COMMENT '响应大小，单位字节',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 索引定义
    INDEX idx_supplier_id (supplier_id) COMMENT '供应商ID索引',
    INDEX idx_trace_id (trace_id) COMMENT '链路追踪ID索引',
    INDEX idx_api_endpoint (api_endpoint) COMMENT 'API端点索引',
    INDEX idx_http_method (http_method) COMMENT 'HTTP方法索引',
    INDEX idx_response_status (response_status) COMMENT '响应状态码索引',
    INDEX idx_response_time_ms (response_time_ms) COMMENT '响应时间索引',
    INDEX idx_is_success (is_success) COMMENT '成功状态索引',
    INDEX idx_business_type (business_type) COMMENT '业务类型索引',
    INDEX idx_channel (channel) COMMENT '调用渠道索引',
    INDEX idx_app_id (app_id) COMMENT '应用ID索引',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引',
    INDEX idx_supplier_endpoint_time (supplier_id, api_endpoint, created_at) COMMENT '供应商端点时间复合索引',
    INDEX idx_status_time (response_status, created_at) COMMENT '状态时间复合索引',
    
    -- 外键约束
    FOREIGN KEY (supplier_id) REFERENCES supplier_config(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API调用日志表';

-- =====================================================
-- 供应商健康检查日志表 - 记录供应商服务健康状态
-- =====================================================
CREATE TABLE `supplier_health_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '健康检查日志ID，主键自增',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID，关联supplier_config表',
  `check_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ping' COMMENT '检查类型：ping、api_test、full_check等',
  `response_time_ms` bigint DEFAULT NULL COMMENT '响应时间，单位毫秒',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `check_details` json DEFAULT NULL COMMENT '检查详情，JSON格式',
  `health_status` enum('UP','DOWN','DEGRADED','UNKNOWN') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '健康状态枚举：HEALTHY、UNHEALTHY、DEGRADED、UNKNOWN',
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

-- =====================================================
-- 系统配置表 - 存储系统级别的配置参数
-- =====================================================
CREATE TABLE system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '系统配置ID，主键自增',
    config_key VARCHAR(200) NOT NULL UNIQUE COMMENT '配置键名，唯一标识',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(50) NOT NULL DEFAULT 'STRING' COMMENT '配置类型：STRING、NUMBER、BOOLEAN、JSON等',
    description TEXT COMMENT '配置描述',
    is_encrypted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否加密存储：1-是，0-否',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1-启用，0-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(100) DEFAULT 'system' COMMENT '创建人',
    updated_by VARCHAR(100) DEFAULT 'system' COMMENT '更新人',
    
    -- 索引定义
    INDEX idx_config_key (config_key) COMMENT '配置键名索引',
    INDEX idx_config_type (config_type) COMMENT '配置类型索引',
    INDEX idx_is_active (is_active) COMMENT '启用状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- =====================================================
-- 创建性能优化视图
-- =====================================================

-- API调用统计视图
CREATE VIEW v_api_call_stats AS
SELECT 
    sc.supplier_name,
    sc.supplier_code,
    acl.business_type,
    DATE(acl.created_at) as call_date,
    COUNT(*) as total_calls,
    SUM(CASE WHEN acl.is_success = 1 THEN 1 ELSE 0 END) as success_calls,
    SUM(CASE WHEN acl.is_success = 0 THEN 1 ELSE 0 END) as failed_calls,
    ROUND(AVG(acl.response_time_ms), 2) as avg_response_time_ms,
    MIN(acl.response_time_ms) as min_response_time_ms,
    MAX(acl.response_time_ms) as max_response_time_ms,
    ROUND(SUM(CASE WHEN acl.is_success = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as success_rate
FROM api_call_log acl
LEFT JOIN supplier_config sc ON acl.supplier_id = sc.id
GROUP BY sc.supplier_name, sc.supplier_code, acl.business_type, DATE(acl.created_at);

-- 供应商健康状态视图
CREATE VIEW v_supplier_health_status AS
SELECT 
    sc.id as supplier_id,
    sc.supplier_name,
    sc.supplier_code,
    sc.is_active as config_active,
    shl.status as health_status,
    shl.response_time_ms as last_check_response_time,
    shl.created_at as last_check_time,
    CASE 
        WHEN sc.is_active = 1 AND shl.status = 'UP' THEN 'HEALTHY'
        WHEN sc.is_active = 1 AND shl.status = 'DEGRADED' THEN 'WARNING'
        WHEN sc.is_active = 0 OR shl.status = 'DOWN' THEN 'UNHEALTHY'
        ELSE 'UNKNOWN'
    END as overall_status
FROM supplier_config sc
LEFT JOIN (
    SELECT 
        supplier_id,
        status,
        response_time_ms,
        created_at,
        ROW_NUMBER() OVER (PARTITION BY supplier_id ORDER BY created_at DESC) as rn
    FROM supplier_health_log
) shl ON sc.id = shl.supplier_id AND shl.rn = 1;
