-- WalkBG MySQL 数据库初始化脚本
-- 注意：此脚本仅在需要初始化示例数据时使用
-- Hibernate 会自动创建表结构（ddl-auto: update）

-- 清理现有路线及关联数据
DELETE FROM user_route_completions;
DELETE FROM user_route_favorites;
DELETE FROM route_images;
DELETE FROM route_tags;
DELETE FROM waypoints;
DELETE FROM segments;
DELETE FROM supplies;
DELETE FROM water_sources;
DELETE FROM campsites;
DELETE FROM route_map_data;
DELETE FROM trips;
DELETE FROM routes;

-- 创建示例用户数据
INSERT INTO users (id, username, email, nickname, avatar_url, phone, created_at, updated_at) VALUES
('user_admin_001', 'admin', 'admin@walkbg.com', '系统管理员', 'https://example.com/avatars/admin.jpg', '13800138000', NOW(), NOW()),
('user_guide_001', 'huangshan_guide', 'guide@huangshan.com', '黄山向导小李', 'https://example.com/avatars/guide.jpg', '13900139000', NOW(), NOW()),
('user_test_001', 'test_user', 'test@walkbg.com', '测试用户', 'https://example.com/avatars/test.jpg', '13700137000', NOW(), NOW());

-- 创建五台山路线数据
INSERT INTO routes (
    id, name, description, region, region_id,
    difficulty, route_type, status, cover_url,
    default_map_id, created_by, popularity, usage_count, is_loop,
    created_at, updated_at
) VALUES (
    'route_wutaishan_001',
    '五台山徒步大朝台路线',
    '五台山五台连穿经典路线，涵盖东台、北台、中台、西台、南台，全程约50-70公里，适合有经验的徒步爱好者。',
    '山西忻州五台山',
    'region_shanxi_wutaishan_001',
    3,
    2,
    1,
    'https://example.com/images/wutaishan_cover.jpg',
    'route_wutaishan_001',
    'user_guide_001',
    100,
    50,
    0,
    NOW(),
    NOW()
);

-- 创建五台山路线地图数据
INSERT INTO route_map_data (
    id, distance, duration, latitude, longitude, altitude,
    elevation_gain, elevation_loss, kml_url, gpx_url,
    favorite_count, completion_count, trip_count,
    created_at, updated_at
) VALUES (
    'route_wutaishan_001',
    60.0,
    1800,
    39.0556,
    113.6565,
    3061.0,
    1800.0,
    1200.0,
    '/static/kml/wutaishan.kml',
    NULL,
    0,
    0,
    0,
    NOW(),
    NOW()
);

-- 创建五台山路线标签
INSERT INTO route_tags (id, route_id, tag) VALUES
('tag_wts_001', 'route_wutaishan_001', '五台山'),
('tag_wts_002', 'route_wutaishan_001', '大朝台'),
('tag_wts_003', 'route_wutaishan_001', '五台连穿'),
('tag_wts_004', 'route_wutaishan_001', '徒步'),
('tag_wts_005', 'route_wutaishan_001', '长线'),
('tag_wts_006', 'route_wutaishan_001', '山西'),
('tag_wts_007', 'route_wutaishan_001', '佛教圣地');

-- 创建五台山路线路点（五台主要地标）
-- 五台连穿经典路线顺序：东台 → 北台 → 中台 → 西台 → 南台
INSERT INTO waypoints (
    id, route_id, name, description,
    latitude, longitude, elevation, type,
    icon_url, image_url, sequence_number,
    created_at, updated_at
) VALUES
-- 东台望海峰
('wpt_wts_001', 'route_wutaishan_001', '东台望海峰', '五台山五台之一，海拔2796米，是观看日出的最佳位置。望海寺供奉聪明文殊菩萨。',
 39.0666, 113.5788, 2796.0, 'summit',
 'https://example.com/icons/summit.png', 'https://example.com/images/wutaishan/dongtai.jpg', 1,
 NOW(), NOW()),

-- 北台叶斗峰（华北屋脊）
('wpt_wts_002', 'route_wutaishan_001', '北台叶斗峰', '五台山最高峰，海拔3061米，被誉为"华北屋脊"。灵应寺供奉无垢文殊菩萨。',
 39.0556, 113.6565, 3061.0, 'summit',
 'https://example.com/icons/summit.png', 'https://example.com/images/wutaishan/beitai.jpg', 2,
 NOW(), NOW()),

-- 中台翠岩峰
('wpt_wts_003', 'route_wutaishan_001', '中台翠岩峰', '五台山五台之一，海拔2894米，山顶有演教寺，供奉儒童文殊菩萨。',
 39.0289, 113.6012, 2894.0, 'summit',
 'https://example.com/icons/summit.png', 'https://example.com/images/wutaishan/zhongtai.jpg', 3,
 NOW(), NOW()),

-- 西台挂月峰
('wpt_wts_004', 'route_wutaishan_001', '西台挂月峰', '五台山五台之一，海拔2773米，法雷寺供奉狮子文殊菩萨。',
 39.0134, 113.5556, 2773.0, 'summit',
 'https://example.com/icons/summit.png', 'https://example.com/images/wutaishan/xitai.jpg', 4,
 NOW(), NOW()),

-- 南台锦绣峰
('wpt_wts_005', 'route_wutaishan_001', '南台锦绣峰', '五台山五台之一，海拔2485米，普济寺供奉智慧文殊菩萨。南台以山花烂漫著称。',
 38.9512, 113.5823, 2485.0, 'summit',
 'https://example.com/icons/summit.png', 'https://example.com/images/wutaishan/nantai.jpg', 5,
 NOW(), NOW()),

-- 鸿门岩（起点）
('wpt_wts_006', 'route_wutaishan_001', '鸿门岩', '五台连穿的传统起点，海拔2500米，是东台与北台之间的山口。',
 39.0712, 113.5834, 2500.0, 'trailhead',
 'https://example.com/icons/trailhead.png', 'https://example.com/images/wutaishan/hongmenyan.jpg', 0,
 NOW(), NOW()),

-- 澡浴池
('wpt_wts_007', 'route_wutaishan_001', '澡浴池', '传说中文殊菩萨沐浴的地方，位于北台和中台之间，是重要的休息补给点。',
 39.0456, 113.6321, 2800.0, 'camp',
 'https://example.com/icons/camp.png', 'https://example.com/images/wutaishan/zaoyuchi.jpg', 6,
 NOW(), NOW()),

-- 狮子窝
('wpt_wts_008', 'route_wutaishan_001', '狮子窝', '位于中西台之间，因寺后有一巨石形如狮子而得名，是朝台路上的重要寺院。',
 39.0212, 113.5876, 2600.0, 'camp',
 'https://example.com/icons/camp.png', 'https://example.com/images/wutaishan/shiziwo.jpg', 7,
 NOW(), NOW()),

-- 金阁寺
('wpt_wts_009', 'route_wutaishan_001', '金阁寺', '位于西台南侧，是五台山著名的佛寺之一，寺内有高达17米的铜铸观音像。',
 39.0023, 113.5712, 2000.0, 'camp',
 'https://example.com/icons/camp.png', 'https://example.com/images/wutaishan/jingesi.jpg', 8,
 NOW(), NOW()),

-- 佛母洞（南台脚下）
('wpt_wts_010', 'route_wutaishan_001', '佛母洞', '位于南台脚下，是朝台路上的著名景点，洞内"重生"体验是朝台的重要仪式。',
 38.9612, 113.5801, 1800.0, 'attraction',
 'https://example.com/icons/attraction.png', 'https://example.com/images/wutaishan/fomu.jpg', 9,
 NOW(), NOW());

-- 创建五台山路线图片
INSERT INTO route_images (id, route_id, image_url, is_cover, sequence_number) VALUES
('img_wts_001', 'route_wutaishan_001', 'https://example.com/images/wutaishan/cover.jpg', 1, 0),
('img_wts_002', 'route_wutaishan_001', 'https://example.com/images/wutaishan/dongtai_sunrise.jpg', 0, 1),
('img_wts_003', 'route_wutaishan_001', 'https://example.com/images/wutaishan/beitai_huabeiwuji.jpg', 0, 2),
('img_wts_004', 'route_wutaishan_001', 'https://example.com/images/wutaishan/zhongtai_yanjiaosi.jpg', 0, 3),
('img_wts_005', 'route_wutaishan_001', 'https://example.com/images/wutaishan/xitai_guayuefeng.jpg', 0, 4),
('img_wts_006', 'route_wutaishan_001', 'https://example.com/images/wutaishan/nantian_jinxiu.jpg', 0, 5);

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

-- 提示信息
SELECT '✅ MySQL 初始化数据插入完成！' AS message;
SELECT CONCAT('共插入 ', COUNT(*), ' 条路线数据') AS routes_count FROM routes;
SELECT CONCAT('共插入 ', COUNT(*), ' 条用户数据') AS users_count FROM users;
SELECT CONCAT('共插入 ', COUNT(*), ' 条攻略数据') AS guides_count FROM guides;
