-- WalkBG MySQL 数据库初始化脚本
-- 注意：此脚本仅在需要初始化示例数据时使用
-- Hibernate 会自动创建表结构（ddl-auto: update）

-- 创建示例用户数据
INSERT INTO users (id, username, email, nickname, avatar_url, phone, created_at, updated_at) VALUES
('user_admin_001', 'admin', 'admin@walkbg.com', '系统管理员', 'https://example.com/avatars/admin.jpg', '13800138000', NOW(), NOW()),
('user_guide_001', 'huangshan_guide', 'guide@huangshan.com', '黄山向导小李', 'https://example.com/avatars/guide.jpg', '13900139000', NOW(), NOW()),
('user_test_001', 'test_user', 'test@walkbg.com', '测试用户', 'https://example.com/avatars/test.jpg', '13700137000', NOW(), NOW());

-- 创建示例路线数据
INSERT INTO routes (
    id, name, description, region, region_id,
    difficulty, route_type, status, cover_url,
    default_map_id, created_by, popularity, usage_count, is_loop,
    created_at, updated_at
) VALUES 
(
    'route_huangshan_001',
    '黄山天都峰徒步路线',
    '黄山天都峰是黄山三大主峰之一，海拔1864米。这条路线从慈光阁出发，经过半山寺、玉屏楼，最终到达天都峰顶。路线风景秀丽，但难度较高，适合有经验的徒步爱好者。',
    '安徽黄山',
    'region_anhui_huangshan_001',
    2,
    0,
    1,
    'https://example.com/images/huangshan_cover.jpg',
    'default_map_001',
    'user_guide_001',
    156,
    89,
    0,
    NOW(),
    NOW()
),
(
    'route_jiuzhaigou_001',
    '九寨沟原始森林徒步',
    '九寨沟原始森林徒步路线，穿越翠海、瀑布群，风景绝美，适合家庭出游。',
    '四川阿坝',
    'region_sichuan_jiuzhai_001',
    1,
    0,
    1,
    'https://example.com/images/jiuzhaigou_cover.jpg',
    'default_map_002',
    'user_guide_001',
    320,
    156,
    0,
    NOW(),
    NOW()
),
(
    'route_zhangjiajie_001',
    '张家界国家森林公园循环',
    '张家界阿凡达同款景点，石峰林立，云雾缭绕，中等难度环线。',
    '湖南张家界',
    'region_hunan_zhangjiajie_001',
    2,
    1,
    1,
    'https://example.com/images/zhangjiajie_cover.jpg',
    'default_map_003',
    'user_guide_001',
    280,
    132,
    1,
    NOW(),
    NOW()
),
(
    'route_changbaishan_001',
    '长白山天池穿越',
    '长白山天池穿越路线，高山火山湖景观，难度较高，需要良好体能。',
    '吉林长白山',
    'region_jilin_changbai_001',
    3,
    2,
    1,
    'https://example.com/images/changbaishan_cover.jpg',
    'default_map_004',
    'user_guide_001',
    198,
    98,
    0,
    NOW(),
    NOW()
),
(
    'route_emeishan_001',
    '峨眉山金顶朝圣',
    '峨眉山金顶朝圣之路，佛教圣地，路途险峻，风景庄严。',
    '四川乐山',
    'region_sichuan_emei_001',
    3,
    0,
    1,
    'https://example.com/images/emeishan_cover.jpg',
    'default_map_005',
    'user_guide_001',
    245,
    118,
    0,
    NOW(),
    NOW()
),
(
    'route_huashan_001',
    '华山长空栈道挑战',
    '华山长空栈道，天下第一险，高难度极限挑战，需签订安全协议。',
    '陕西华阴',
    'region_shaanxi_huashan_001',
    4,
    0,
    1,
    'https://example.com/images/huashan_cover.jpg',
    'default_map_006',
    'user_guide_001',
    412,
    208,
    0,
    NOW(),
    NOW()
),
(
    'route_taishan_001',
    '泰山日出之路',
    '泰山日出观景路线，五岳之首，文化底蕴深厚，难度适中。',
    '山东泰安',
    'region_shandong_taishan_001',
    2,
    0,
    1,
    'https://example.com/images/taishan_cover.jpg',
    'default_map_007',
    'user_guide_001',
    385,
    192,
    0,
    NOW(),
    NOW()
),
(
    'route_wuyi_001',
    '武夷山九曲溪漂流',
    '武夷山九曲溪漂流+徒步组合，丹霞地貌，轻松休闲。',
    '福建武夷山',
    'region_fujian_wuyishan_001',
    1,
    1,
    1,
    'https://example.com/images/wuyishan_cover.jpg',
    'default_map_008',
    'user_guide_001',
    167,
    78,
    1,
    NOW(),
    NOW()
),
(
    'route_guilin_001',
    '桂林漓江竹箏漂流',
    '桂林山水甲天下，漓江竹箏漂流+岸上徒步，风景如画。',
    '广西桂林',
    'region_guangxi_guilin_001',
    1,
    0,
    1,
    'https://example.com/images/guilin_cover.jpg',
    'default_map_009',
    'user_guide_001',
    298,
    145,
    0,
    NOW(),
    NOW()
),
(
    'route_xihu_001',
    '西湖十景环湖漫步',
    '杭州西湖十景环湖漫步，轻松休闲，适合全家出游。',
    '浙江杭州',
    'region_zhejiang_xihu_001',
    1,
    1,
    1,
    'https://example.com/images/xihu_cover.jpg',
    'default_map_010',
    'user_guide_001',
    456,
    256,
    1,
    NOW(),
    NOW()
);

-- 创建示例路径点数据
INSERT INTO waypoints (id, name, description, latitude, longitude, elevation, waypoint_type, icon_url, image_url, sequence_number, route_id, created_at, updated_at) VALUES
('waypoint_001', '慈光阁', '黄山南大门，徒步起点，海拔800米', 30.1394, 118.1558, 800.0, '起点', 'https://example.com/icons/start.png', 'https://example.com/images/ciguangge.jpg', 1, 'route_huangshan_001', NOW(), NOW()),
('waypoint_002', '半山寺', '黄山著名古寺，徒步中转点，海拔1340米', 30.1425, 118.1612, 1340.0, '中转点', 'https://example.com/icons/temple.png', 'https://example.com/images/banshan.jpg', 2, 'route_huangshan_001', NOW(), NOW()),
('waypoint_003', '玉屏楼', '黄山著名景点，迎客松所在地，海拔1680米', 30.1456, 118.1634, 1680.0, '景点', 'https://example.com/icons/scenic.png', 'https://example.com/images/yupinglou.jpg', 3, 'route_huangshan_001', NOW(), NOW()),
('waypoint_004', '天都峰顶', '黄山三大主峰之一，海拔1864米，视野开阔', 30.1478, 118.1645, 1864.0, '终点', 'https://example.com/icons/peak.png', 'https://example.com/images/tiandufeng.jpg', 4, 'route_huangshan_001', NOW(), NOW());

-- 创建示例路段数据
INSERT INTO segments (id, name, description, distance, elevation_gain, elevation_loss, estimated_time, difficulty, route_id, created_at, updated_at) VALUES
('segment_001', '慈光阁至半山寺', '石阶路段，坡度适中', 3.2, 540.0, 0.0, 2.5, 1, 'route_huangshan_001', NOW(), NOW()),
('segment_002', '半山寺至玉屏楼', '山路段，有一定难度', 2.8, 340.0, 0.0, 2.0, 2, 'route_huangshan_001', NOW(), NOW()),
('segment_003', '玉屏楼至天都峰顶', '陡峭岩石路段，难度较高', 1.5, 184.0, 0.0, 1.5, 3, 'route_huangshan_001', NOW(), NOW());

-- 创建示例标签数据
INSERT INTO route_tags (id, tag, route_id) VALUES
(UUID(), '山峰', 'route_huangshan_001'),
(UUID(), '风景名胜', 'route_huangshan_001'),
(UUID(), '高难度', 'route_huangshan_001'),
(UUID(), '一日游', 'route_huangshan_001'),
(UUID(), '摄影', 'route_huangshan_001');

-- 创建示例图片数据
INSERT INTO route_images (id, image_url, is_cover, sequence_number, route_id) VALUES
(UUID(), 'https://example.com/images/huangshan_cover.jpg', 1, 1, 'route_huangshan_001'),
(UUID(), 'https://example.com/images/huangshan_sunrise.jpg', 0, 2, 'route_huangshan_001'),
(UUID(), 'https://example.com/images/huangshan_clouds.jpg', 0, 3, 'route_huangshan_001'),
(UUID(), 'https://example.com/images/yingkesong.jpg', 0, 4, 'route_huangshan_001');

-- 创建示例补给点数据
INSERT INTO supplies (
    id, name, description, route_id, latitude, longitude, elevation,
    supply_type, created_at, updated_at
) VALUES
(
    'supply_001',
    '云谷寺商店',
    '云谷寺入口处的综合商店，提供登山用品和食物',
    'route_huangshan_001',
    30.1234,
    118.1567,
    630.0,
    1,
    NOW(),
    NOW()
),
(
    'supply_002',
    '慈光阁补给站',
    '慈光阁附近的补给点，提供基础物资',
    'route_huangshan_001',
    30.1394,
    118.1558,
    800.0,
    0,
    NOW(),
    NOW()
);

-- 创建示例水源数据
INSERT INTO water_sources (
    id, name, description, route_id, latitude, longitude, elevation,
    water_type, water_quality, reliability, requires_treatment, notes,
    created_at, updated_at
) VALUES
(
    'water_001',
    '半山寺泉水',
    '半山寺附近的天然泉水，水质清澈甘甜',
    'route_huangshan_001',
    30.1425,
    118.1612,
    1340.0,
    0,
    0,
    0.9,
    0,
    '位于半山寺后方约50米处，24小时可取水',
    NOW(),
    NOW()
),
(
    'water_002',
    '天海补水点',
    '天海宾馆附近的人工水源',
    'route_huangshan_001',
    30.1384,
    118.1717,
    1750.0,
    1,
    1,
    0.8,
    1,
    '天海宾馆提供的饮用水，建议过滤后饮用，需付费',
    NOW(),
    NOW()
);

-- 创建示例营地数据
INSERT INTO campsites (
    id, name, description, route_id, latitude, longitude, elevation,
    campsite_type, capacity, facilities, created_at, updated_at
) VALUES
(
    'campsite_001',
    '天海营地',
    '黄山著名营地，设施完善',
    'route_huangshan_001',
    30.1384,
    118.1717,
    1750.0,
    1,
    50,
    '帐篷区,卫生间,补给站',
    NOW(),
    NOW()
);

-- 创建示例攻略数据
INSERT INTO guides (
    id, title, content, author_id, cover_url,
    tags, view_count, like_count, status,
    created_at, updated_at
) VALUES
(
    'guide_huangshan_001',
    '黄山徒步完全指南',
    '黄山是中国著名的山峰，以奇松、怪石、云海、温泉闻名于世。本攻略详细介绍了黄山的各条徒步路线、最佳观赏季节、装备准备、注意事项等内容。适合初次到黄山的徒步爱好者阅读。',
    'user_guide_001',
    'https://example.com/images/guide_huangshan.jpg',
    '登山技巧,装备指南,路线规划',
    1250,
    89,
    1,
    NOW(),
    NOW()
),
(
    'guide_jiuzhaigou_001',
    '九寨沟摄影攻略',
    '九寨沟是世界自然遗产，以翠海、叠瀑、彩林、雪峽闻名。本攻略介绍了九寨沟的最佳摄影点、拍摄时间、相机设置等内容，帮助您拍摄出令人惊艳的照片。',
    'user_guide_001',
    'https://example.com/images/guide_jiuzhaigou.jpg',
    '摄影技巧,旅行攻略,景点推荐',
    2340,
    156,
    1,
    NOW(),
    NOW()
),
(
    'guide_camping_001',
    '户外露营完全手册',
    '露营是亲近大自然的最好方式。本手册详细介绍了露营装备选择、营地选择、帐篷搭建、野外烹饪、安全注意事项等内容，适合露营新手阅读。',
    'user_guide_001',
    'https://example.com/images/guide_camping.jpg',
    '露营,户外装备,新手指南',
    3560,
    234,
    1,
    NOW(),
    NOW()
);

-- 创建示例行程数据
INSERT INTO trips (
    id, name, description, organizer_id,
    start_date, end_date, status, privacy_setting,
    max_participants, current_participants,
    created_at, updated_at
) VALUES
(
    'trip_huangshan_001',
    '黄山天都峰三日游',
    '三天时间徒步黄山天都峰，体验奇松怪石云海的魅力。包含山上住宿和导游服务。',
    'user_guide_001',
    DATE_ADD(NOW(), INTERVAL 30 DAY),
    DATE_ADD(NOW(), INTERVAL 32 DAY),
    0,
    0,
    15,
    5,
    NOW(),
    NOW()
),
(
    'trip_jiuzhaigou_001',
    '九寨沟摄影团',
    '专业摄影师带队，深入九寨沟核心景区，捕捉最美的瞬间。',
    'user_guide_001',
    DATE_ADD(NOW(), INTERVAL 60 DAY),
    DATE_ADD(NOW(), INTERVAL 64 DAY),
    0,
    0,
    10,
    3,
    NOW(),
    NOW()
),
(
    'trip_camping_weekend_001',
    '周末露营体验',
    '周末两天一夜的露营体验，适合新手和家庭参与。提供全套装备租赁服务。',
    'user_test_001',
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    DATE_ADD(NOW(), INTERVAL 8 DAY),
    0,
    1,
    20,
    12,
    NOW(),
    NOW()
);

-- 创建示例路线地图数据（KML/GPX 和统计信息）
INSERT INTO route_map_data (
    id, distance, duration, latitude, longitude, altitude,
    elevation_gain, elevation_loss, kml_url, gpx_url,
    favorite_count, completion_count, trip_count,
    created_at, updated_at
) VALUES
(
    'route_huangshan_001',
    7.5,
    360,
    30.1450,
    118.1600,
    1864.0,
    1064.0,
    0.0,
    'https://example.com/maps/huangshan_001.kml',
    'https://example.com/maps/huangshan_001.gpx',
    342,
    89,
    12,
    NOW(),
    NOW()
),
(
    'route_jiuzhaigou_001',
    12.0,
    480,
    33.2520,
    104.7702,
    2400.0,
    450.0,
    450.0,
    'https://example.com/maps/jiuzhaigou_001.kml',
    'https://example.com/maps/jiuzhaigou_001.gpx',
    578,
    156,
    28,
    NOW(),
    NOW()
),
(
    'route_zhangjiajie_001',
    15.3,
    540,
    29.3255,
    110.4625,
    1200.0,
    600.0,
    600.0,
    'https://example.com/maps/zhangjiajie_001.kml',
    'https://example.com/maps/zhangjiajie_001.gpx',
    456,
    132,
    35,
    NOW(),
    NOW()
),
(
    'route_changbaishan_001',
    18.5,
    600,
    42.0002,
    128.0562,
    2744.0,
    950.0,
    950.0,
    'https://example.com/maps/changbaishan_001.kml',
    'https://example.com/maps/changbaishan_001.gpx',
    385,
    98,
    22,
    NOW(),
    NOW()
),
(
    'route_emeishan_001',
    20.2,
    720,
    29.5432,
    103.3344,
    3099.0,
    1500.0,
    0.0,
    'https://example.com/maps/emeishan_001.kml',
    'https://example.com/maps/emeishan_001.gpx',
    512,
    118,
    38,
    NOW(),
    NOW()
),
(
    'route_huashan_001',
    16.8,
    600,
    34.4868,
    110.0843,
    2155.0,
    1200.0,
    0.0,
    'https://example.com/maps/huashan_001.kml',
    'https://example.com/maps/huashan_001.gpx',
    723,
    208,
    65,
    NOW(),
    NOW()
),
(
    'route_taishan_001',
    22.4,
    780,
    36.2597,
    117.1205,
    1545.0,
    1345.0,
    0.0,
    'https://example.com/maps/taishan_001.kml',
    'https://example.com/maps/taishan_001.gpx',
    678,
    192,
    72,
    NOW(),
    NOW()
),
(
    'route_wuyi_001',
    11.5,
    420,
    27.7431,
    117.4845,
    800.0,
    300.0,
    300.0,
    'https://example.com/maps/wuyi_001.kml',
    'https://example.com/maps/wuyi_001.gpx',
    334,
    78,
    18,
    NOW(),
    NOW()
),
(
    'route_guilin_001',
    24.6,
    840,
    25.2919,
    110.2965,
    500.0,
    150.0,
    150.0,
    'https://example.com/maps/guilin_001.kml',
    'https://example.com/maps/guilin_001.gpx',
    612,
    145,
    52,
    NOW(),
    NOW()
),
(
    'route_xihu_001',
    13.0,
    480,
    30.2875,
    120.1551,
    300.0,
    50.0,
    50.0,
    'https://example.com/maps/xihu_001.kml',
    'https://example.com/maps/xihu_001.gpx',
    945,
    256,
    98,
    NOW(),
    NOW()
);

-- 添加更多九寨沟路线的路径点数据
INSERT INTO waypoints (id, name, description, latitude, longitude, elevation, type, icon_url, image_url, sequence_number, route_id, created_at, updated_at) VALUES
('waypoint_jiuzhai_001', '诺日朗瀑布', '九寨沟最大的瀑布，落差20米', 33.2685, 104.7743, 2365.0, '景点', 'https://example.com/icons/waterfall.png', 'https://example.com/images/nuorilang.jpg', 1, 'route_jiuzhaigou_001', NOW(), NOW()),
('waypoint_jiuzhai_002', '五花海', '九寨沟最美的海子，色彩斑斓', 33.2542, 104.7634, 2472.0, '景点', 'https://example.com/icons/lake.png', 'https://example.com/images/wuhuahai.jpg', 2, 'route_jiuzhaigou_001', NOW(), NOW()),
('waypoint_jiuzhai_003', '镜海', '湖面平静如镜，倒影清晰', 33.2720, 104.7912, 2340.0, '景点', 'https://example.com/icons/lake.png', 'https://example.com/images/jinghai.jpg', 3, 'route_jiuzhaigou_001', NOW(), NOW()),
('waypoint_jiuzhai_004', '箭竹海', '竹海风景独特', 33.2956, 104.7589, 2588.0, '景点', 'https://example.com/icons/scenic.png', 'https://example.com/images/jianzhushi.jpg', 4, 'route_jiuzhaigou_001', NOW(), NOW()),
('waypoint_jiuzhai_005', '长海', '九寨沟最高最大的海子', 33.3156, 104.7312, 2590.0, '终点', 'https://example.com/icons/peak.png', 'https://example.com/images/changhai.jpg', 5, 'route_jiuzhaigou_001', NOW(), NOW());

-- 添加九寨沟路线段数据
INSERT INTO segments (id, name, description, distance, elevation_gain, elevation_loss, estimated_time, difficulty, route_id, created_at, updated_at) VALUES
('segment_jiuzhai_001', '诺日朗至五花海', '平坦步道', 2.5, 107.0, 0.0, 1.0, 0, 'route_jiuzhaigou_001', NOW(), NOW()),
('segment_jiuzhai_002', '五花海至镜海', '缓下坡', 3.2, 0.0, 132.0, 1.2, 0, 'route_jiuzhaigou_001', NOW(), NOW()),
('segment_jiuzhai_003', '镜海至箭竹海', '上升路段', 2.8, 248.0, 0.0, 1.5, 1, 'route_jiuzhaigou_001', NOW(), NOW()),
('segment_jiuzhai_004', '箭竹海至长海', '缓上升', 3.5, 2.0, 0.0, 1.3, 0, 'route_jiuzhaigou_001', NOW(), NOW());

-- 添加九寨沟路线标签
INSERT INTO route_tags (id, tag, route_id) VALUES
(UUID(), '世界遗产', 'route_jiuzhaigou_001'),
(UUID(), '水景', 'route_jiuzhaigou_001'),
(UUID(), '摄影胜地', 'route_jiuzhaigou_001'),
(UUID(), '轻松', 'route_jiuzhaigou_001'),
(UUID(), '彩林', 'route_jiuzhaigou_001');

-- 添加九寨沟路线图片
INSERT INTO route_images (id, image_url, is_cover, sequence_number, route_id) VALUES
(UUID(), 'https://example.com/images/jiuzhaigou_cover.jpg', 1, 1, 'route_jiuzhaigou_001'),
(UUID(), 'https://example.com/images/jiuzhaigou_spring.jpg', 0, 2, 'route_jiuzhaigou_001'),
(UUID(), 'https://example.com/images/jiuzhaigou_fall.jpg', 0, 3, 'route_jiuzhaigou_001'),
(UUID(), 'https://example.com/images/jiuzhaigou_waterfall.jpg', 0, 4, 'route_jiuzhaigou_001');

-- 添加九寨沟补给点
INSERT INTO supplies (
    id, name, description, route_id, latitude, longitude, elevation,
    supply_type, created_at, updated_at
) VALUES
(
    'supply_jiuzhai_001',
    '诺日朗服务中心',
    '九寨沟内最大的服务中心，提供餐饮和补给',
    'route_jiuzhaigou_001',
    33.2685,
    104.7743,
    2365.0,
    1,
    NOW(),
    NOW()
),
(
    'supply_jiuzhai_002',
    '原始森林便利店',
    '提供登山食品和饮用水',
    'route_jiuzhaigou_001',
    33.2850,
    104.7600,
    2480.0,
    0,
    NOW(),
    NOW()
);

-- 添加九寨沟水源
INSERT INTO water_sources (
    id, name, description, route_id, latitude, longitude, elevation,
    water_type, water_quality, reliability, requires_treatment, notes,
    created_at, updated_at
) VALUES
(
    'water_jiuzhai_001',
    '五花海补水点',
    '景区内提供的饮用水补给点',
    'route_jiuzhaigou_001',
    33.2542,
    104.7634,
    2472.0,
    1,
    1,
    0.95,
    0,
    '由景区管理，水质有保障',
    NOW(),
    NOW()
),
(
    'water_jiuzhai_002',
    '诺日朗瀑布水源',
    '自然水源，需处理',
    'route_jiuzhaigou_001',
    33.2685,
    104.7743,
    2365.0,
    0,
    0,
    0.8,
    1,
    '瀑布水需过滤和加热处理',
    NOW(),
    NOW()
);

-- 添加用户路线收藏
INSERT INTO user_route_favorites (user_id, route_id, created_at) VALUES
('user_test_001', 'route_huangshan_001', NOW()),
('user_test_001', 'route_jiuzhaigou_001', NOW()),
('user_admin_001', 'route_huashan_001', NOW()),
('user_admin_001', 'route_xihu_001', NOW());

-- 添加用户路线完成记录
INSERT INTO user_route_completions (user_id, route_id, completed_at, duration_minutes, notes, created_at, updated_at) VALUES
('user_guide_001', 'route_huangshan_001', DATE_SUB(NOW(), INTERVAL 15 DAY), 340, '天气晴朗，风景壮观', NOW(), NOW()),
('user_guide_001', 'route_jiuzhaigou_001', DATE_SUB(NOW(), INTERVAL 30 DAY), 480, '彩林正当时，拍摄效果很好', NOW(), NOW()),
('user_test_001', 'route_xihu_001', DATE_SUB(NOW(), INTERVAL 5 DAY), 120, '轻松惬意的散步', NOW(), NOW());

-- 提示信息
SELECT '✅ MySQL 初始化数据插入完成！' AS message;
SELECT CONCAT('共插入 ', COUNT(*), ' 条路线数据') AS routes_count FROM routes;
SELECT CONCAT('共插入 ', COUNT(*), ' 条用户数据') AS users_count FROM users;
SELECT CONCAT('共插入 ', COUNT(*), ' 条路径点数据') AS waypoints_count FROM waypoints;
SELECT CONCAT('共插入 ', COUNT(*), ' 条攻略数据') AS guides_count FROM guides;
SELECT CONCAT('共插入 ', COUNT(*), ' 条行程数据') AS trips_count FROM trips;
SELECT CONCAT('共插入 ', COUNT(*), ' 条路线地图数据') AS route_map_data_count FROM route_map_data;
