-- Hotel Supplier Integration Service Database Schema
-- Version 1.0 - Initial tables creation

-- 供应商配置表
CREATE TABLE supplier_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(100) NOT NULL UNIQUE,
    api_base_url VARCHAR(500) NOT NULL,
    auth_type VARCHAR(50) NOT NULL DEFAULT 'MD5',
    auth_config TEXT,
    timeout BIGINT NOT NULL DEFAULT 30000,
    retry_count INT NOT NULL DEFAULT 3,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_supplier_name (supplier_name),
    INDEX idx_is_active (is_active),
    INDEX idx_priority (priority)
);

-- 酒店信息表
CREATE TABLE hotel_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id BIGINT NOT NULL,
    supplier_hotel_id VARCHAR(100) NOT NULL,
    hotel_name VARCHAR(200) NOT NULL,
    address TEXT,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    star_rating DECIMAL(2,1),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    description TEXT,
    amenities TEXT,
    images TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_supplier_hotel (supplier_id, supplier_hotel_id),
    INDEX idx_city (city),
    INDEX idx_country (country),
    INDEX idx_star_rating (star_rating),
    INDEX idx_is_active (is_active),
    INDEX idx_location (latitude, longitude),
    FOREIGN KEY (supplier_id) REFERENCES supplier_config(id) ON DELETE CASCADE
);

-- 房间信息表
CREATE TABLE room_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    supplier_room_id VARCHAR(100) NOT NULL,
    room_name VARCHAR(200) NOT NULL,
    room_type VARCHAR(100),
    bed_type VARCHAR(100),
    max_occupancy INT,
    room_size INT,
    has_window BOOLEAN,
    has_private_bathroom BOOLEAN,
    smoking_allowed BOOLEAN,
    amenities TEXT,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_hotel_room (hotel_id, supplier_room_id),
    INDEX idx_room_type (room_type),
    INDEX idx_bed_type (bed_type),
    INDEX idx_max_occupancy (max_occupancy),
    INDEX idx_is_active (is_active),
    FOREIGN KEY (hotel_id) REFERENCES hotel_info(id) ON DELETE CASCADE
);

-- 预订记录表
CREATE TABLE booking_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_reference VARCHAR(100) NOT NULL UNIQUE,
    supplier_booking_id VARCHAR(100),
    distributor_order_id VARCHAR(100),
    supplier_id BIGINT NOT NULL,
    hotel_id BIGINT NOT NULL,
    guest_name VARCHAR(200) NOT NULL,
    guest_email VARCHAR(200),
    guest_phone VARCHAR(50),
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    room_count INT NOT NULL DEFAULT 1,
    guest_count INT NOT NULL DEFAULT 1,
    total_amount DECIMAL(10,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'MYR',
    booking_status INT NOT NULL DEFAULT 1,
    channel VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_booking_reference (booking_reference),
    INDEX idx_supplier_booking_id (supplier_booking_id),
    INDEX idx_distributor_order_id (distributor_order_id),
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_hotel_id (hotel_id),
    INDEX idx_guest_name (guest_name),
    INDEX idx_booking_status (booking_status),
    INDEX idx_check_in_date (check_in_date),
    INDEX idx_check_out_date (check_out_date),
    INDEX idx_channel (channel),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (supplier_id) REFERENCES supplier_config(id),
    FOREIGN KEY (hotel_id) REFERENCES hotel_info(id)
);

-- API调用日志表
CREATE TABLE api_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id BIGINT,
    api_endpoint VARCHAR(500) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_data TEXT,
    response_data TEXT,
    response_status INT,
    response_time_ms BIGINT,
    error_message TEXT,
    channel VARCHAR(50),
    client_ip VARCHAR(45),
    app_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_api_endpoint (api_endpoint),
    INDEX idx_http_method (http_method),
    INDEX idx_response_status (response_status),
    INDEX idx_response_time_ms (response_time_ms),
    INDEX idx_channel (channel),
    INDEX idx_app_id (app_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (supplier_id) REFERENCES supplier_config(id) ON DELETE SET NULL
);

-- 创建视图：活跃酒店统计
CREATE VIEW active_hotels_stats AS
SELECT 
    sc.supplier_name,
    hi.city,
    hi.country,
    COUNT(*) as hotel_count,
    AVG(hi.star_rating) as avg_star_rating,
    MIN(hi.star_rating) as min_star_rating,
    MAX(hi.star_rating) as max_star_rating
FROM hotel_info hi
JOIN supplier_config sc ON hi.supplier_id = sc.id
WHERE hi.is_active = TRUE AND sc.is_active = TRUE
GROUP BY sc.supplier_name, hi.city, hi.country;

-- 创建视图：预订统计
CREATE VIEW booking_stats AS
SELECT 
    DATE(br.created_at) as booking_date,
    sc.supplier_name,
    br.booking_status,
    COUNT(*) as booking_count,
    SUM(br.total_amount) as total_amount,
    AVG(br.total_amount) as avg_amount,
    br.currency
FROM booking_record br
JOIN supplier_config sc ON br.supplier_id = sc.id
GROUP BY DATE(br.created_at), sc.supplier_name, br.booking_status, br.currency;
