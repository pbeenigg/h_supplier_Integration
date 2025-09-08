-- =====================================================
-- HeyTrip 酒店供应商集成服务初始化数据
-- 创建时间: 2025-09-08
-- 作者: Pax
-- =====================================================

-- =====================================================
-- 插入供应商配置数据
-- =====================================================
INSERT INTO supplier_config (
    supplier_name, 
    supplier_code, 
    api_base_url, 
    auth_type, 
    auth_config, 
    timeout_ms, 
    retry_count, 
    max_concurrent_requests,
    rate_limit_per_second,
    is_active, 
    priority,
    description,
    contact_info,
    supported_countries,
    supported_cities,
    created_by,
    updated_by
) VALUES
(
    'AsianOverland', 
    'AO_QTECH', 
    'https://colosseum.otrams.com/ws/index.php', 
    'MD5', 
    '{"appId":"Heytrip_Test","secretKey":"Welcome@@123","username":"Heytrip_Test","password":"Welcome@@123"}', 
    30000, 
    3, 
    10,
    50,
    TRUE, 
    10,
    'AsianOverland供应商通过QTECH技术通道提供马来西亚酒店资源',
    '{"email":"support@asianoverland.com","phone":"+60-3-1234-5678","contact_person":"技术支持团队"}',
    '["MY"]',
    '["Kuala Lumpur","Penang","Johor Bahru","Malacca","Ipoh","Kota Kinabalu","Kuching"]',
    'system',
    'system'
),
(
    'TestSupplier', 
    'TEST_DEMO', 
    'https://api.testsupplier.com/v1', 
    'MD5', 
    '{"appId":"test_app_demo","secretKey":"demo_secret_key_123"}', 
    25000, 
    2, 
    5,
    20,
    FALSE, 
    20,
    '测试供应商，用于开发和测试环境',
    '{"email":"test@testsupplier.com","phone":"+1-555-0123","contact_person":"Test Support"}',
    '["US","CA"]',
    '["New York","Los Angeles","Toronto","Vancouver"]',
    'system',
    'system'
);

-- =====================================================
-- 插入系统配置数据
-- =====================================================
INSERT INTO system_config (
    config_key,
    config_value,
    config_type,
    description,
    is_encrypted,
    is_active,
    created_by,
    updated_by
) VALUES
('system.default.timeout_ms', '30000', 'NUMBER', '系统默认API调用超时时间（毫秒）', FALSE, TRUE, 'system', 'system'),
('system.default.retry_count', '3', 'NUMBER', '系统默认重试次数', FALSE, TRUE, 'system', 'system'),
('system.max_concurrent_requests', '100', 'NUMBER', '系统最大并发请求数', FALSE, TRUE, 'system', 'system'),
('system.health_check.interval_seconds', '60', 'NUMBER', '健康检查间隔时间（秒）', FALSE, TRUE, 'system', 'system'),
('system.log.retention_days', '30', 'NUMBER', 'API调用日志保留天数', FALSE, TRUE, 'system', 'system'),
('system.cache.enabled', 'true', 'BOOLEAN', '是否启用缓存', FALSE, TRUE, 'system', 'system'),
('system.cache.ttl_seconds', '300', 'NUMBER', '缓存过期时间（秒）', FALSE, TRUE, 'system', 'system'),
('system.security.encryption_key', 'heytrip_secret_key_2025', 'STRING', '系统加密密钥', TRUE, TRUE, 'system', 'system'),
('system.notification.email.enabled', 'false', 'BOOLEAN', '是否启用邮件通知', FALSE, TRUE, 'system', 'system'),
('system.notification.webhook.url', '', 'STRING', 'Webhook通知地址', FALSE, FALSE, 'system', 'system');

-- =====================================================
-- 插入示例API调用日志数据
-- =====================================================
INSERT INTO api_call_log (
    supplier_id, 
    trace_id,
    api_endpoint, 
    http_method, 
    request_headers,
    request_params,
    request_body, 
    response_headers,
    response_body, 
    response_status, 
    response_time_ms,
    error_code,
    error_message,
    retry_count,
    is_success,
    business_type,
    channel, 
    client_ip, 
    user_agent,
    app_id,
    user_id,
    session_id,
    request_size_bytes,
    response_size_bytes
) VALUES
(
    1, 
    'trace-001-20250908-001',
    '/hotel/search', 
    'POST', 
    '{"Content-Type":"application/json","Authorization":"Bearer token123"}',
    '{"supplierType":"AO_QTECH"}',
    '{"city":"Kuala Lumpur","checkInDate":"2025-02-15","checkOutDate":"2025-02-18","roomCount":1,"adultCount":2}', 
    '{"Content-Type":"application/json","Server":"nginx/1.18.0"}',
    '{"code":0,"message":"success","data":{"hotels":[{"hotelId":"AO_KL_001","hotelName":"Grand Hyatt Kuala Lumpur"}]}}', 
    200, 
    1250,
    NULL,
    NULL,
    0,
    TRUE,
    'hotel_search',
    'API', 
    '192.168.1.100', 
    'HeyTrip-Client/1.0',
    'heytrip_web_app',
    'user_12345',
    'session_abc123',
    512,
    2048
),
(
    1, 
    'trace-002-20250908-002',
    '/booking/create', 
    'POST', 
    '{"Content-Type":"application/json","Authorization":"Bearer token123"}',
    '{"supplierType":"AO_QTECH"}',
    '{"hotelId":"AO_KL_001","roomId":"AO_KL_001_R001","ratePlanId":"RP_001","guestName":"John Smith","checkInDate":"2025-02-15","checkOutDate":"2025-02-18"}', 
    '{"Content-Type":"application/json","Server":"nginx/1.18.0"}',
    '{"code":0,"message":"success","data":{"bookingReference":"AO_BK_001","supplierOrderId":"SO_001","status":"CONFIRMED"}}', 
    200, 
    2100,
    NULL,
    NULL,
    0,
    TRUE,
    'booking_create',
    'API', 
    '192.168.1.100', 
    'HeyTrip-Client/1.0',
    'heytrip_web_app',
    'user_12345',
    'session_abc123',
    768,
    1024
),
(
    1, 
    'trace-003-20250908-003',
    '/hotel/search', 
    'POST', 
    '{"Content-Type":"application/json","Authorization":"Bearer token456"}',
    '{"supplierType":"AO_QTECH"}',
    '{"city":"Penang","checkInDate":"2025-03-01","checkOutDate":"2025-03-05","roomCount":2,"adultCount":4}', 
    '{"Content-Type":"application/json","Server":"nginx/1.18.0"}',
    '{"code":0,"message":"success","data":{"hotels":[{"hotelId":"AO_PG_001","hotelName":"Eastern & Oriental Hotel"}]}}', 
    200, 
    980,
    NULL,
    NULL,
    0,
    TRUE,
    'hotel_search',
    'API', 
    '192.168.1.101', 
    'HeyTrip-Mobile/2.1',
    'heytrip_mobile_app',
    'user_67890',
    'session_def456',
    456,
    1536
),
(
    1, 
    'trace-004-20250908-004',
    '/booking/cancel', 
    'POST', 
    '{"Content-Type":"application/json","Authorization":"Bearer token789"}',
    '{"supplierType":"AO_QTECH"}',
    '{"bookingReference":"AO_BK_005","reason":"Customer request","cancelReason":"Change of plans"}', 
    '{"Content-Type":"application/json","Server":"nginx/1.18.0"}',
    '{"code":0,"message":"success","data":{"cancelled":true,"refundAmount":500.00,"currency":"MYR"}}', 
    200, 
    1800,
    NULL,
    NULL,
    0,
    TRUE,
    'booking_cancel',
    'API', 
    '192.168.1.102', 
    'HeyTrip-Client/1.0',
    'heytrip_web_app',
    'user_11111',
    'session_ghi789',
    384,
    512
),
(
    2, 
    'trace-005-20250908-005',
    '/api/v1/hotels', 
    'GET', 
    '{"Content-Type":"application/json","X-API-Key":"demo_key"}',
    '{"city":"New York","checkin":"2025-04-01","checkout":"2025-04-03"}',
    NULL, 
    '{"Content-Type":"application/json","Server":"Apache/2.4.41"}',
    '{"error":"Supplier not available","code":503}', 
    503, 
    5000,
    'SUPPLIER_UNAVAILABLE',
    'Supplier service is temporarily unavailable',
    2,
    FALSE,
    'hotel_search',
    'API', 
    '10.0.0.50', 
    'HeyTrip-Client/1.0',
    'heytrip_web_app',
    'user_22222',
    'session_jkl012',
    256,
    128
);

-- =====================================================
-- 插入供应商健康检查日志数据
-- =====================================================
INSERT INTO supplier_health_log (
    supplier_id,
    check_type,
    health_status,
    response_time_ms,
    error_message,
    check_details
) VALUES
(1, 'ping', 'UP', 150, NULL, '{"endpoint":"/health","method":"GET","timestamp":"2025-09-08T15:30:00Z"}'),
(1, 'api_test', 'UP', 800, NULL, '{"test_endpoint":"/hotel/search","sample_request_success":true,"timestamp":"2025-09-08T15:30:30Z"}'),
(2, 'ping', 'DOWN', NULL, 'Connection timeout', '{"endpoint":"/health","method":"GET","error":"timeout after 5000ms","timestamp":"2025-09-08T15:31:00Z"}'),
(2, 'api_test', 'DOWN', NULL, 'Service unavailable', '{"test_endpoint":"/api/v1/status","error":"503 Service Unavailable","timestamp":"2025-09-08T15:31:30Z"}');

-- =====================================================
-- 创建额外的性能优化索引
-- =====================================================

-- API调用日志表的额外索引
CREATE INDEX idx_api_log_trace_id ON api_call_log(trace_id);
CREATE INDEX idx_api_log_business_success ON api_call_log(business_type, is_success);
CREATE INDEX idx_api_log_response_time_range ON api_call_log(response_time_ms) WHERE response_time_ms > 1000;

-- 供应商健康检查日志表的额外索引  
CREATE INDEX idx_health_log_status_time ON supplier_health_log(status, created_at);
CREATE INDEX idx_health_log_check_type_supplier ON supplier_health_log(check_type, supplier_id);

-- =====================================================
-- 插入初始化完成标记
-- =====================================================
INSERT INTO system_config (
    config_key,
    config_value,
    config_type,
    description,
    is_encrypted,
    is_active,
    created_by,
    updated_by
) VALUES
('system.init.completed', 'true', 'BOOLEAN', '系统初始化完成标记', FALSE, TRUE, 'system', 'system'),
('system.init.version', '2.0', 'STRING', '系统初始化版本', FALSE, TRUE, 'system', 'system'),
('system.init.timestamp', NOW(), 'STRING', '系统初始化时间戳', FALSE, TRUE, 'system', 'system');
