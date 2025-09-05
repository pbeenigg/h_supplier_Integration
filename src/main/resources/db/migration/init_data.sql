-- Hotel Supplier Integration Service Initial Data
-- Version 2.0 - Insert initial configuration and sample data

-- 插入供应商配置
INSERT INTO supplier_config (supplier_name, api_base_url, auth_type, auth_config, timeout, retry_count, is_active, priority) VALUES
('AsianOverland', 'https://api.asianoverland.com', 'MD5', '{"appId":"test_app_id","secretKey":"test_secret_key"}', 30000, 3, TRUE, 10),
('TestSupplier', 'https://api.testsupplier.com', 'MD5', '{"appId":"test_app_2","secretKey":"test_secret_2"}', 25000, 2, FALSE, 20);

-- 插入示例酒店数据 (AsianOverland供应商)
INSERT INTO hotel_info (supplier_id, supplier_hotel_id, hotel_name, address, city, country, star_rating, latitude, longitude, description, amenities, is_active) VALUES
(1, 'AO_KL_001', 'Grand Millennium Kuala Lumpur', '160, Jalan Bukit Bintang, Bukit Bintang, 55100 Kuala Lumpur', 'Kuala Lumpur', 'Malaysia', 5.0, 3.1478, 101.7089, 'Luxury hotel in the heart of Kuala Lumpur', 'WiFi,Pool,Gym,Spa,Restaurant,Bar', TRUE),
(1, 'AO_KL_002', 'Hotel Maya Kuala Lumpur', '138, Jalan Ampang, Kuala Lumpur City Centre, 50450 Kuala Lumpur', 'Kuala Lumpur', 'Malaysia', 5.0, 3.1570, 101.7123, 'Contemporary luxury hotel with modern amenities', 'WiFi,Pool,Gym,Spa,Restaurant,Business Center', TRUE),
(1, 'AO_PG_001', 'Eastern & Oriental Hotel', '10, Lebuh Farquhar, George Town, 10200 George Town, Penang', 'Penang', 'Malaysia', 5.0, 5.4164, 100.3327, 'Historic colonial hotel with heritage charm', 'WiFi,Pool,Spa,Restaurant,Heritage Tours', TRUE),
(1, 'AO_JB_001', 'Renaissance Johor Bahru Hotel', 'No. 2, Jalan Permas 11, Bandar Baru Permas Jaya, 81750 Masai, Johor', 'Johor Bahru', 'Malaysia', 4.5, 1.4927, 103.8018, 'Modern business hotel near Singapore border', 'WiFi,Pool,Gym,Restaurant,Business Center', TRUE),
(1, 'AO_ML_001', 'The Majestic Malacca', '188, Jalan Bunga Raya, 75100 Melaka', 'Malacca', 'Malaysia', 5.0, 2.1896, 102.2501, 'Heritage luxury hotel in historic Malacca', 'WiFi,Pool,Spa,Restaurant,Heritage Tours,Museum', TRUE);

-- 插入示例房间数据
INSERT INTO room_info (hotel_id, supplier_room_id, room_name, room_type, bed_type, max_occupancy, room_size, has_window, has_private_bathroom, smoking_allowed, amenities, is_active) VALUES
-- Grand Millennium Kuala Lumpur rooms
(1, 'AO_KL_001_R001', 'Deluxe Room', 'Deluxe', 'King Bed', 2, 35, TRUE, TRUE, FALSE, 'WiFi,AC,TV,Minibar,Safe', TRUE),
(1, 'AO_KL_001_R002', 'Premier Room', 'Premier', 'King Bed', 2, 40, TRUE, TRUE, FALSE, 'WiFi,AC,TV,Minibar,Safe,City View', TRUE),
(1, 'AO_KL_001_R003', 'Executive Suite', 'Suite', 'King Bed', 4, 60, TRUE, TRUE, FALSE, 'WiFi,AC,TV,Minibar,Safe,Living Area,City View', TRUE),

-- Hotel Maya Kuala Lumpur rooms
(2, 'AO_KL_002_R001', 'Maya Room', 'Standard', 'King Bed', 2, 32, TRUE, TRUE, FALSE, 'WiFi,AC,TV,Minibar,Safe', TRUE),
(2, 'AO_KL_002_R002', 'Maya Premier', 'Premier', 'King Bed', 2, 38, TRUE, TRUE, FALSE, 'WiFi,AC,TV,Minibar,Safe,Pool View', TRUE),

-- Eastern & Oriental Hotel rooms
(3, 'AO_PG_001_R001', 'Heritage Room', 'Heritage', 'Queen Bed', 2, 30, TRUE, TRUE, FALSE, 'WiFi,AC,TV,Minibar,Safe,Heritage Decor', TRUE),
(3, 'AO_PG_001_R002', 'Victory Suite', 'Suite', 'King Bed', 4, 55, TRUE, TRUE, FALSE, 'WiFi,AC,TV,Minibar,Safe,Living Area,Sea View', TRUE),

-- Renaissance Johor Bahru Hotel rooms
(4, 'AO_JB_001_R001', 'Deluxe Room', 'Deluxe', 'King Bed', 2, 33, TRUE, TRUE, FALSE, 'WiFi,AC,TV,Minibar,Safe,Work Desk', TRUE),
(4, 'AO_JB_001_R002', 'Club Level Room', 'Club', 'King Bed', 2, 36, TRUE, TRUE, FALSE, 'WiFi,AC,TV,Minibar,Safe,Club Lounge Access', TRUE),

-- The Majestic Malacca rooms
(5, 'AO_ML_001_R001', 'Heritage Deluxe', 'Heritage Deluxe', 'King Bed', 2, 34, TRUE, TRUE, FALSE, 'WiFi,AC,TV,Minibar,Safe,Heritage Decor', TRUE),
(5, 'AO_ML_001_R002', 'Majestic Suite', 'Suite', 'King Bed', 4, 65, TRUE, TRUE, FALSE, 'WiFi,AC,TV,Minibar,Safe,Living Area,River View', TRUE);

-- 插入示例预订记录
INSERT INTO booking_record (booking_reference, supplier_booking_id, distributor_order_id, supplier_id, hotel_id, guest_name, guest_email, guest_phone, check_in_date, check_out_date, room_count, guest_count, total_amount, currency, booking_status, channel) VALUES
('HT20240101001', 'AO_BK_001', 'HT1704067200001', 1, 1, 'John Smith', 'john.smith@email.com', '+60123456789', '2024-02-15', '2024-02-18', 1, 2, 450.00, 'MYR', 2, 'API'),
('HT20240101002', 'AO_BK_002', 'HT1704067200002', 1, 2, 'Jane Doe', 'jane.doe@email.com', '+60198765432', '2024-02-20', '2024-02-22', 1, 1, 320.00, 'MYR', 2, 'API'),
('HT20240101003', 'AO_BK_003', 'HT1704067200003', 1, 3, 'Bob Johnson', 'bob.johnson@email.com', '+60187654321', '2024-03-01', '2024-03-05', 2, 4, 800.00, 'MYR', 1, 'API'),
('HT20240101004', 'AO_BK_004', 'HT1704067200004', 1, 4, 'Alice Brown', 'alice.brown@email.com', '+60176543210', '2024-03-10', '2024-03-12', 1, 2, 280.00, 'MYR', 2, 'API'),
('HT20240101005', 'AO_BK_005', 'HT1704067200005', 1, 5, 'Charlie Wilson', 'charlie.wilson@email.com', '+60165432109', '2024-03-15', '2024-03-20', 1, 3, 650.00, 'MYR', 9, 'API');

-- 插入示例API调用日志
INSERT INTO api_call_log (supplier_id, api_endpoint, http_method, request_data, response_data, response_status, response_time_ms, channel, client_ip, app_id) VALUES
(1, '/hotel/search', 'POST', '{"city":"Kuala Lumpur","checkInDate":"2024-02-15","checkOutDate":"2024-02-18"}', '{"code":0,"hotels":[...]}', 200, 1250, 'API', '192.168.1.100', 'test_app_id'),
(1, '/booking/create', 'POST', '{"hotelId":"AO_KL_001","roomId":"AO_KL_001_R001","guestName":"John Smith"}', '{"code":0,"bookingReference":"AO_BK_001"}', 200, 2100, 'API', '192.168.1.100', 'test_app_id'),
(1, '/hotel/search', 'POST', '{"city":"Penang","checkInDate":"2024-03-01","checkOutDate":"2024-03-05"}', '{"code":0,"hotels":[...]}', 200, 980, 'API', '192.168.1.101', 'test_app_id'),
(1, '/booking/cancel', 'POST', '{"bookingReference":"AO_BK_005","reason":"Customer request"}', '{"code":0,"cancelled":true}', 200, 1800, 'API', '192.168.1.102', 'test_app_id');

-- 创建索引优化查询性能
CREATE INDEX idx_hotel_city_active ON hotel_info(city, is_active);
CREATE INDEX idx_booking_dates ON booking_record(check_in_date, check_out_date);
CREATE INDEX idx_api_log_supplier_endpoint ON api_call_log(supplier_id, api_endpoint);
CREATE INDEX idx_booking_status_created ON booking_record(booking_status, created_at);
