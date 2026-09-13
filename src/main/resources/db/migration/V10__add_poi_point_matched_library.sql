-- KML 分析识别点关联全局 POI 库：记录命中的库内条目
-- 命中关系在分析回调落库时由 AI 判定产生；NULL 表示新点（未命中或判定回退）
ALTER TABLE poi_points ADD COLUMN matched_library_id VARCHAR(64) NULL;
