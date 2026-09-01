-- 迁移脚本：路线软删除支持（配合 walkbg 57e69db+）
-- 背景：Route 实体新增 is_deleted 字段，生产环境 ddl-auto=validate 不会自动建列，
--       部署前必须先在本库执行本脚本，否则应用启动时 schema 校验失败（502）。
-- 执行方式：mysql -u root -p walkbg < scripts/migrate-routes-soft-delete.sql
-- 幂等性：ALTER 重复执行会报 Duplicate column，属正常，可忽略。

ALTER TABLE routes
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '软删除标记: 0正常 1已删除';

CREATE INDEX idx_routes_is_deleted ON routes (is_deleted);

-- 验证
SELECT COUNT(*) AS total, SUM(is_deleted) AS deleted FROM routes;
