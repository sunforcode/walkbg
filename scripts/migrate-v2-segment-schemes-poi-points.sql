-- =============================================================================
-- WalkBG 数据模型 v2 迁移脚本
-- 变更：新增 segment_schemes 表、poi_points 表、segments 表新增字段
-- 适用：生产环境（ddl-auto: validate）手动执行
-- 开发/测试环境（ddl-auto: update）由 JPA 自动执行，无需此脚本
-- =============================================================================

USE walkbg;

-- -----------------------------------------------------------------------------
-- 1. 新建 segment_schemes 表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS segment_schemes (
    id          VARCHAR(64)  NOT NULL PRIMARY KEY,
    route_id    VARCHAR(64)  NOT NULL COMMENT '所属路线 ID',
    scheme_type VARCHAR(32)  NOT NULL COMMENT '方案类型：slope|day|terrain|road_type',
    label       VARCHAR(64)  NOT NULL COMMENT '展示用标签，如"按坡度"',
    is_default  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否为默认方案',
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_segment_schemes_route_id  (route_id),
    INDEX idx_segment_schemes_type      (scheme_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='路线分段方案表，每套方案代表一种轨迹染色维度';

-- -----------------------------------------------------------------------------
-- 2. segments 表新增字段
-- -----------------------------------------------------------------------------
ALTER TABLE segments
    ADD COLUMN IF NOT EXISTS scheme_id   VARCHAR(64) NULL COMMENT 'FK → segment_schemes.id',
    ADD COLUMN IF NOT EXISTS scheme_type VARCHAR(32) NULL COMMENT '冗余：所属方案类型';

-- 建索引（如尚不存在）
CREATE INDEX IF NOT EXISTS idx_segments_scheme_id   ON segments (scheme_id);
CREATE INDEX IF NOT EXISTS idx_segments_scheme_type ON segments (scheme_type);

-- -----------------------------------------------------------------------------
-- 3. 新建 poi_points 表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS poi_points (
    id           VARCHAR(64)   NOT NULL PRIMARY KEY,
    route_id     VARCHAR(64)   NOT NULL COMMENT '所属路线 ID',
    name         VARCHAR(200)  NOT NULL,
    latitude     DOUBLE        NOT NULL,
    longitude    DOUBLE        NOT NULL,
    elevation    DOUBLE        NULL,
    category     VARCHAR(32)   NOT NULL COMMENT 'water|camp|supply|photo|pass|valley|weather|danger|start|end',
    sub_category VARCHAR(64)   NULL      COMMENT '细分类型',
    source       VARCHAR(32)   NOT NULL  COMMENT 'kml_marker|algorithm|osm|weather_api|experience',
    description  TEXT          NULL,
    confidence   DOUBLE        NULL      COMMENT '数据置信度 0-1',
    card_data    TEXT          NULL      COMMENT '各 category 扩展属性 JSON，walkbg 不解析直接透传',
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_poi_points_route_id       (route_id),
    INDEX idx_poi_points_category       (category),
    INDEX idx_poi_points_route_category (route_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='统一附属信息点表，替代旧的 campsites/water_sources/supplies/marker_points';

-- -----------------------------------------------------------------------------
-- 完成提示
-- -----------------------------------------------------------------------------
SELECT '✅ 数据模型 v2 迁移完成' AS status;
