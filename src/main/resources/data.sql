-- 路线数据插入脚本
-- 可以在H2控制台中直接执行

-- 插入路线数据
INSERT INTO routes (id, name, description, region, distance, duration, difficulty, popularity, created_at, updated_at)
VALUES 
('route-001', '九寨沟徒步路线', '九寨沟是中国著名的自然保护区，以其多彩的湖泊、瀑布和原始森林闻名。', '四川省阿坝藏族羌族自治州', 12.5, '7小时', 2, 12, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
('route-002', '张家界森林公园路线', '张家界以其独特的石英砂岩峰林地貌闻名于世，是《阿凡达》电影的灵感来源地。', '湖南省张家界市', 9.8, '6小时', 2, 14, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
('route-003', '长白山天池路线', '长白山天池是中朝边境的一个火山口湖，是松花江、图们江、鸭绿江的源头。', '吉林省延边朝鲜族自治州', 7.5, '5小时', 3, 9, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

-- 插入路线季节数据
INSERT INTO route_seasons (id, route_id, season, created_at, updated_at)
VALUES 
(UUID(), 'route-001', '夏季', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-001', '秋季', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-002', '春季', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-002', '秋季', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-003', '夏季', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

-- 插入路线标签数据
INSERT INTO route_tags (id, route_id, tag, created_at, updated_at)
VALUES 
(UUID(), 'route-001', '湖泊', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-001', '瀑布', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-001', '森林', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-002', '山岳', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-002', '奇石', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-003', '火山', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-003', '湖泊', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

-- 插入路线评分数据
INSERT INTO route_ratings (id, route_id, average_rating, rating_count, created_at, updated_at)
VALUES 
(UUID(), 'route-001', 4.7, 156, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-002', 4.8, 203, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-003', 4.5, 98, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

-- 插入路线图片数据
INSERT INTO route_images (id, route_id, image_url, is_cover, sequence_number, created_at, updated_at)
VALUES 
(UUID(), 'route-001', 'https://example.com/images/jiuzhaigou1.jpg', true, 1, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-001', 'https://example.com/images/jiuzhaigou2.jpg', false, 2, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-002', 'https://example.com/images/zhangjiajie1.jpg', true, 1, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(UUID(), 'route-003', 'https://example.com/images/changbaishan1.jpg', true, 1, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

-- 注意：UUID()函数在H2数据库中可用，用于生成唯一标识符
-- 如果遇到问题，可以使用具体的UUID字符串替代