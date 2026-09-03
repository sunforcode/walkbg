-- Add the schema required by the global POI library feature introduced after V1.
-- Existing route POIs and segments predate draft review, so preserve their
-- published behavior by backfilling them as confirmed.

ALTER TABLE poi_points
    ADD COLUMN status varchar(20) NOT NULL DEFAULT 'confirmed';

ALTER TABLE segments
    ADD COLUMN status varchar(20) NOT NULL DEFAULT 'confirmed';

ALTER TABLE routes
    ADD COLUMN track_geo_json TEXT;

CREATE TABLE poi_library (
    latitude float(53) NOT NULL,
    longitude float(53) NOT NULL,
    elevation float(53),
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    category varchar(32) NOT NULL,
    id varchar(64) NOT NULL,
    region_id varchar(64),
    source_route_id varchar(64),
    status varchar(20) NOT NULL DEFAULT 'active',
    region_name varchar(100),
    sub_category varchar(64),
    name varchar(200) NOT NULL,
    ai_reason TEXT,
    description TEXT,
    PRIMARY KEY (id),
    INDEX idx_poi_library_name (name),
    INDEX idx_poi_library_status (status)
) ENGINE=InnoDB;
