-- WalkBG 数据库初始化脚本
-- 创建示例用户数据
INSERT INTO users (id, username, email, nickname, avatar_url, phone, is_active, created_at, updated_at) VALUES
('user_admin_001', 'admin', 'admin@walkbg.com', '系统管理员', 'https://example.com/avatars/admin.jpg', '13800138000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user_guide_001', 'huangshan_guide', 'guide@huangshan.com', '黄山向导小李', 'https://example.com/avatars/guide.jpg', '13900139000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user_test_001', 'test_user', 'test@walkbg.com', '测试用户', 'https://example.com/avatars/test.jpg', '13700137000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 创建示例路线数据
INSERT INTO routes (
    id, name, description, region, region_id, distance, duration, 
    latitude, longitude, altitude, elevation_gain, elevation_loss, 
    difficulty, route_type, route_direction, status, cover_url, 
    map_data_id, default_map_id, created_by, popularity, 
    created_at, updated_at
) VALUES (
    'route_huangshan_001',
    '黄山天都峰徒步路线',
    '黄山天都峰是黄山三大主峰之一，海拔1864米。这条路线从慈光阁出发，经过半山寺、玉屏楼，最终到达天都峰顶。路线风景秀丽，但难度较高，适合有经验的徒步爱好者。',
    '安徽黄山',
    'region_anhui_huangshan_001',
    12.50,
    8,
    30.1394,
    118.1558,
    800.0,
    1064.0,
    200.0,
    2,
    0,
    0,
    1,
    'https://example.com/images/huangshan_cover.jpg',
    'map_huangshan_001',
    'default_map_001',
    'user_guide_001',
    156,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 创建示例路径点数据
INSERT INTO waypoints (id, name, description, latitude, longitude, elevation, type, icon_url, image_url, sequence_number, route_id, created_at, updated_at) VALUES
('waypoint_001', '慈光阁', '黄山南大门，徒步起点，海拔800米', 30.1394, 118.1558, 800.0, '起点', 'https://example.com/icons/start.png', 'https://example.com/images/ciguangge.jpg', 1, 'route_huangshan_001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('waypoint_002', '半山寺', '黄山著名古寺，徒步中转点，海拔1340米', 30.1425, 118.1612, 1340.0, '中转点', 'https://example.com/icons/temple.png', 'https://example.com/images/banshan.jpg', 2, 'route_huangshan_001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('waypoint_003', '玉屏楼', '黄山著名景点，迎客松所在地，海拔1680米', 30.1456, 118.1634, 1680.0, '景点', 'https://example.com/icons/scenic.png', 'https://example.com/images/yupinglou.jpg', 3, 'route_huangshan_001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('waypoint_004', '天都峰顶', '黄山三大主峰之一，海拔1864米，视野开阔', 30.1478, 118.1645, 1864.0, '终点', 'https://example.com/icons/peak.png', 'https://example.com/images/tiandufeng.jpg', 4, 'route_huangshan_001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 创建示例路段数据
INSERT INTO segments (id, distance, elevation_gain, elevation_loss, estimated_time, difficulty, terrain, surface_type, traffic_level, route_id, created_at, updated_at) VALUES
('segment_001', 3.2, 540.0, 0.0, 2.5, 1, '石阶路', '石阶', 2, 'route_huangshan_001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('segment_002', 2.8, 340.0, 0.0, 2.0, 2, '山路', '土路', 1, 'route_huangshan_001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('segment_003', 1.5, 184.0, 0.0, 1.5, 3, '陡峭岩石', '岩石', 0, 'route_huangshan_001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 创建示例标签数据
INSERT INTO route_tags (tag, route_id) VALUES
('山峰', 'route_huangshan_001'),
('风景名胜', 'route_huangshan_001'),
('高难度', 'route_huangshan_001'),
('一日游', 'route_huangshan_001'),
('摄影', 'route_huangshan_001');

-- 创建示例季节数据
INSERT INTO route_seasons (season, route_id) VALUES
('春季', 'route_huangshan_001'),
('夏季', 'route_huangshan_001'),
('秋季', 'route_huangshan_001');

-- 创建示例图片数据
INSERT INTO route_images (image_url, is_cover, sequence_number, route_id) VALUES
('https://example.com/images/huangshan_cover.jpg', true, 1, 'route_huangshan_001'),
('https://example.com/images/huangshan_sunrise.jpg', false, 2, 'route_huangshan_001'),
('https://example.com/images/huangshan_clouds.jpg', false, 3, 'route_huangshan_001'),
('https://example.com/images/yingkesong.jpg', false, 4, 'route_huangshan_001');

-- 创建示例补给点数据
INSERT INTO supplies (
    id, name, description, route_id, latitude, longitude, elevation,
    supply_type, last_verified, updated_by, created_at, updated_at
) VALUES
(
    'sp_001',
    '云谷寺商店',
    '云谷寺入口处的综合商店，提供登山用品和食物',
    'route_huangshan_001',
    30.1234,
    118.1567,
    630.0,
    1,
    '2023-05-20T10:30:00Z',
    'user_001',
    '2023-05-15T08:00:00Z',
    '2023-05-20T10:30:00Z'
),
(
    'sp_002',
    '慈光阁补给站',
    '慈光阁附近的补给点，提供基础物资',
    'route_huangshan_001',
    30.1394,
    118.1558,
    800.0,
    0,
    '2023-05-18T14:20:00Z',
    'user_guide_001',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 创建示例水源数据
INSERT INTO water_sources (
    id, name, description, route_id, latitude, longitude, elevation,
    water_type, water_quality, reliability, requires_treatment, notes,
    last_verified, verified_by, created_at, updated_at
) VALUES
(
    'ws_001',
    '半山寺泉水',
    '半山寺附近的天然泉水，水质清澈甘甜',
    'route_huangshan_001',
    30.1425,
    118.1612,
    1340.0,
    0,
    0,
    0.9,
    false,
    '位于半山寺后方约50米处，24小时可取水',
    '2023-05-15T14:30:00Z',
    'user_guide_002',
    '2023-05-10T08:00:00Z',
    '2023-05-15T14:30:00Z'
),
(
    'ws_002',
    '天海补水点',
    '天海宾馆附近的人工水源',
    'route_huangshan_001',
    30.1384,
    118.1717,
    1750.0,
    1,
    1,
    0.8,
    true,
    '天海宾馆提供的饮用水，建议过滤后饮用，需付费',
    '2023-05-20T10:30:00Z',
    'user_002',
    '2023-05-15T08:00:00Z',
    '2023-05-20T10:30:00Z'
);
