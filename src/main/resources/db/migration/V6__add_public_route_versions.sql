CREATE TABLE route_versions (
    id varchar(64) NOT NULL,
    route_id varchar(64) NOT NULL,
    version_label varchar(200),
    route_type varchar(32),
    name varchar(200),
    region varchar(100),
    start_name varchar(200),
    end_name varchar(200),
    estimated_duration_seconds bigint,
    difficulty varchar(100),
    direction varchar(100),
    distance_meters decimal(14,3),
    ascent_meters decimal(14,3),
    descent_meters decimal(14,3),
    max_elevation_meters decimal(14,3),
    suggested_days integer,
    tags_json LONGTEXT,
    introduction TEXT,
    professional_analysis_json LONGTEXT,
    reference_days_json LONGTEXT,
    seasonal_weather_json LONGTEXT,
    seasonal_equipment_recommendations_json LONGTEXT,
    main_track_availability varchar(32) NOT NULL,
    main_track_reference_system varchar(64),
    main_track_json LONGTEXT,
    created_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_route_versions_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT ck_route_versions_route_type CHECK (route_type IN ('one_day', 'multi_day')),
    CONSTRAINT ck_route_versions_suggested_days CHECK (suggested_days IS NULL OR suggested_days > 0),
    CONSTRAINT ck_route_versions_track_availability CHECK (
        main_track_availability IN ('missing', 'processing', 'pending_review', 'valid', 'invalidated')
    )
) ENGINE=InnoDB;

CREATE INDEX idx_route_versions_route ON route_versions (route_id);

CREATE TABLE route_current_public_versions (
    route_id varchar(64) NOT NULL,
    route_version_id varchar(64) NOT NULL,
    PRIMARY KEY (route_id),
    CONSTRAINT uk_current_public_route_version UNIQUE (route_version_id),
    CONSTRAINT fk_current_public_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT fk_current_public_version FOREIGN KEY (route_version_id) REFERENCES route_versions (id)
) ENGINE=InnoDB;

CREATE TABLE public_route_collection (
    route_id varchar(64) NOT NULL,
    all_route_order integer NOT NULL,
    featured_order integer,
    PRIMARY KEY (route_id),
    CONSTRAINT uk_public_route_all_order UNIQUE (all_route_order),
    CONSTRAINT uk_public_route_featured_order UNIQUE (featured_order),
    CONSTRAINT fk_public_route_collection_route FOREIGN KEY (route_id) REFERENCES routes (id)
) ENGINE=InnoDB;

CREATE TABLE route_version_images (
    id varchar(64) NOT NULL,
    route_version_id varchar(64) NOT NULL,
    media_reference varchar(500) NOT NULL,
    role varchar(32) NOT NULL,
    display_order integer NOT NULL,
    caption varchar(500),
    PRIMARY KEY (id),
    CONSTRAINT fk_route_version_images_version FOREIGN KEY (route_version_id) REFERENCES route_versions (id),
    CONSTRAINT ck_route_version_images_role CHECK (role IN ('cover', 'environment'))
) ENGINE=InnoDB;

CREATE INDEX idx_route_version_images_version ON route_version_images (route_version_id);
CREATE UNIQUE INDEX uk_route_version_images_order ON route_version_images (route_version_id, display_order);

CREATE TABLE route_version_segments (
    id varchar(64) NOT NULL,
    route_version_id varchar(64) NOT NULL,
    segment_order integer NOT NULL,
    name varchar(200) NOT NULL,
    start_name varchar(200),
    end_name varchar(200),
    distance_meters decimal(14,3),
    estimated_duration_seconds bigint,
    ascent_meters decimal(14,3),
    descent_meters decimal(14,3),
    difficulty varchar(100),
    terrain_or_road_type varchar(100),
    description TEXT,
    notes TEXT,
    main_track_range_json TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_route_version_segments_version FOREIGN KEY (route_version_id) REFERENCES route_versions (id)
) ENGINE=InnoDB;

CREATE INDEX idx_route_version_segments_version ON route_version_segments (route_version_id);
CREATE UNIQUE INDEX uk_route_version_segments_order ON route_version_segments (route_version_id, segment_order);

CREATE TABLE route_version_points (
    id varchar(64) NOT NULL,
    route_version_id varchar(64) NOT NULL,
    point_kind varchar(32) NOT NULL,
    display_order integer NOT NULL,
    name varchar(200) NOT NULL,
    category varchar(100),
    sub_category varchar(100),
    description TEXT,
    latitude float(53) NOT NULL,
    longitude float(53) NOT NULL,
    elevation float(53),
    reference_system varchar(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_route_version_points_version FOREIGN KEY (route_version_id) REFERENCES route_versions (id)
) ENGINE=InnoDB;

CREATE INDEX idx_route_version_points_version ON route_version_points (route_version_id);
CREATE UNIQUE INDEX uk_route_version_points_order ON route_version_points (route_version_id, display_order);

-- The legacy route/map schema records route distance in kilometres, elevation in metres,
-- and route duration in minutes. The version snapshot converts only those established units.
-- Legacy KML/GPX paths have WGS84 coordinates, but an existing published route is not
-- automatically treated as reviewed: migrated paths remain pending_review until explicit review.
INSERT INTO route_versions (
    id,
    route_id,
    version_label,
    route_type,
    name,
    region,
    start_name,
    end_name,
    estimated_duration_seconds,
    difficulty,
    direction,
    distance_meters,
    ascent_meters,
    descent_meters,
    max_elevation_meters,
    suggested_days,
    tags_json,
    introduction,
    professional_analysis_json,
    reference_days_json,
    seasonal_weather_json,
    seasonal_equipment_recommendations_json,
    main_track_availability,
    main_track_reference_system,
    main_track_json,
    created_at
)
SELECT
    CONCAT('rv-', LEFT(SHA2(CONCAT('initial:', r.id), 256), 61)),
    r.id,
    'initial',
    NULL,
    NULLIF(TRIM(r.name), ''),
    NULLIF(TRIM(r.region), ''),
    CASE
        WHEN (SELECT COUNT(*) FROM poi_points start_poi WHERE start_poi.route_id = r.id AND start_poi.status = 'confirmed' AND start_poi.category = 'start' AND NULLIF(TRIM(start_poi.name), '') IS NOT NULL) = 1
        THEN (SELECT MAX(NULLIF(TRIM(start_poi.name), '')) FROM poi_points start_poi WHERE start_poi.route_id = r.id AND start_poi.status = 'confirmed' AND start_poi.category = 'start')
        ELSE NULL
    END,
    CASE
        WHEN (SELECT COUNT(*) FROM poi_points end_poi WHERE end_poi.route_id = r.id AND end_poi.status = 'confirmed' AND end_poi.category = 'end' AND NULLIF(TRIM(end_poi.name), '') IS NOT NULL) = 1
        THEN (SELECT MAX(NULLIF(TRIM(end_poi.name), '')) FROM poi_points end_poi WHERE end_poi.route_id = r.id AND end_poi.status = 'confirmed' AND end_poi.category = 'end')
        ELSE NULL
    END,
    CASE WHEN md.duration >= 0 THEN md.duration * 60 ELSE NULL END,
    CASE r.difficulty
        WHEN 1 THEN '1'
        WHEN 2 THEN '2'
        WHEN 3 THEN '3'
        WHEN 4 THEN '4'
        WHEN 5 THEN '5'
        ELSE NULL
    END,
    CASE r.route_type
        WHEN 0 THEN '往返'
        WHEN 1 THEN '环线'
        WHEN 2 THEN '单程'
        ELSE NULL
    END,
    CASE WHEN md.distance >= 0 THEN md.distance * 1000 ELSE NULL END,
    CASE WHEN md.elevation_gain >= 0 THEN md.elevation_gain ELSE NULL END,
    CASE WHEN md.elevation_loss >= 0 THEN md.elevation_loss ELSE NULL END,
    CASE WHEN md.altitude >= 0 THEN md.altitude ELSE NULL END,
    NULL,
    NULL,
    NULLIF(TRIM(r.description), ''),
    NULL,
    NULL,
    NULL,
    NULL,
    CASE WHEN r.track_geo_json IS NULL OR TRIM(r.track_geo_json) = '' THEN 'missing' ELSE 'pending_review' END,
    CASE WHEN r.track_geo_json IS NULL OR TRIM(r.track_geo_json) = '' THEN NULL ELSE 'WGS84' END,
    CASE WHEN r.track_geo_json IS NULL OR TRIM(r.track_geo_json) = '' THEN NULL ELSE r.track_geo_json END,
    r.created_at
FROM routes r
LEFT JOIN route_map_data md ON md.id = r.id;

INSERT INTO route_current_public_versions (route_id, route_version_id)
SELECT r.id, CONCAT('rv-', LEFT(SHA2(CONCAT('initial:', r.id), 256), 61))
FROM routes r
JOIN route_versions version ON version.route_id = r.id
WHERE r.status = 1
  AND r.is_deleted = b'0'
  AND version.route_type IN ('one_day', 'multi_day')
  AND version.name IS NOT NULL
  AND version.region IS NOT NULL
  AND version.estimated_duration_seconds IS NOT NULL
  AND version.start_name IS NOT NULL
  AND version.end_name IS NOT NULL
  AND version.main_track_availability = 'valid';

SET @all_route_order := 0;
INSERT INTO public_route_collection (route_id, all_route_order, featured_order)
SELECT ordered.id, (@all_route_order := @all_route_order + 1), NULL
FROM (
    SELECT r.id
    FROM routes r
    JOIN route_current_public_versions current_version ON current_version.route_id = r.id
    WHERE r.status = 1 AND r.is_deleted = b'0'
    ORDER BY r.created_at ASC, r.id ASC
) ordered;

INSERT INTO route_version_images (id, route_version_id, media_reference, role, display_order, caption)
SELECT
    CONCAT('rvi-', LEFT(SHA2(CONCAT('legacy-image:', ri.id), 256), 60)),
    CONCAT('rv-', LEFT(SHA2(CONCAT('initial:', ri.route_id), 256), 61)),
    ri.image_url,
    CASE WHEN ri.is_cover = b'1' THEN 'cover' ELSE 'environment' END,
    ROW_NUMBER() OVER (PARTITION BY ri.route_id ORDER BY ri.sequence_number ASC, ri.id ASC),
    NULL
FROM route_images ri
WHERE NULLIF(TRIM(ri.image_url), '') IS NOT NULL
  AND NOT (
      ri.is_cover = b'1'
      AND EXISTS (
          SELECT 1
          FROM route_images earlier
          WHERE earlier.route_id = ri.route_id
            AND earlier.is_cover = b'1'
            AND (earlier.sequence_number < ri.sequence_number OR (earlier.sequence_number = ri.sequence_number AND earlier.id < ri.id))
      )
  );

INSERT INTO route_version_images (id, route_version_id, media_reference, role, display_order, caption)
SELECT
    CONCAT('rvi-', LEFT(SHA2(CONCAT('legacy-cover:', r.id), 256), 60)),
    CONCAT('rv-', LEFT(SHA2(CONCAT('initial:', r.id), 256), 61)),
    r.cover_url,
    'cover',
    0,
    NULL
FROM routes r
WHERE NULLIF(TRIM(r.cover_url), '') IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM route_images ri WHERE ri.route_id = r.id AND ri.is_cover = b'1'
  );

INSERT INTO route_version_segments (
    id,
    route_version_id,
    segment_order,
    name,
    start_name,
    end_name,
    distance_meters,
    estimated_duration_seconds,
    ascent_meters,
    descent_meters,
    difficulty,
    terrain_or_road_type,
    description,
    notes,
    main_track_range_json
)
SELECT
    CONCAT('rvs-', LEFT(SHA2(CONCAT('legacy-segment:', s.id), 256), 60)),
    CONCAT('rv-', LEFT(SHA2(CONCAT('initial:', s.route_id), 256), 61)),
    ROW_NUMBER() OVER (PARTITION BY s.route_id ORDER BY s.sequence_number ASC, s.id ASC),
    s.name,
    NULLIF(TRIM(start_waypoint.name), ''),
    NULLIF(TRIM(end_waypoint.name), ''),
    CASE WHEN s.distance >= 0 THEN s.distance * 1000 ELSE NULL END,
    CASE WHEN s.estimated_time >= 0 THEN ROUND(s.estimated_time * 60) ELSE NULL END,
    CASE WHEN s.elevation_gain >= 0 THEN s.elevation_gain ELSE NULL END,
    CASE WHEN s.elevation_loss >= 0 THEN s.elevation_loss ELSE NULL END,
    CASE s.difficulty
        WHEN 1 THEN '1'
        WHEN 2 THEN '2'
        WHEN 3 THEN '3'
        WHEN 4 THEN '4'
        WHEN 5 THEN '5'
        ELSE NULL
    END,
    NULLIF(TRIM(s.scheme_type), ''),
    NULLIF(TRIM(s.description), ''),
    NULLIF(TRIM(s.notes), ''),
    CASE
        WHEN s.track_start_index IS NOT NULL
         AND s.track_end_index IS NOT NULL
         AND s.track_start_index >= 0
         AND s.track_end_index > s.track_start_index
        THEN JSON_OBJECT(
            'startPathPosition', JSON_OBJECT('precedingPositionIndex', s.track_start_index, 'progressToNextPosition', 0),
            'endPathPosition', JSON_OBJECT('precedingPositionIndex', s.track_end_index, 'progressToNextPosition', 0)
        )
        ELSE NULL
    END
FROM segments s
LEFT JOIN waypoints start_waypoint ON start_waypoint.id = s.start_point_id
LEFT JOIN waypoints end_waypoint ON end_waypoint.id = s.end_point_id
WHERE s.status = 'confirmed'
  AND NULLIF(TRIM(s.name), '') IS NOT NULL;

INSERT INTO route_version_points (
    id,
    route_version_id,
    point_kind,
    display_order,
    name,
    category,
    sub_category,
    description,
    latitude,
    longitude,
    elevation,
    reference_system
)
SELECT
    CONCAT('rvp-', LEFT(SHA2(CONCAT('legacy-poi:', p.id), 256), 60)),
    CONCAT('rv-', LEFT(SHA2(CONCAT('initial:', p.route_id), 256), 61)),
    CASE p.category
        WHEN 'start' THEN 'start'
        WHEN 'end' THEN 'end'
        WHEN 'camp' THEN 'campsite'
        WHEN 'water' THEN 'water_source'
        WHEN 'supply' THEN 'supply_point'
        WHEN 'danger' THEN 'safety_notice'
        WHEN 'pass' THEN 'key'
        WHEN 'valley' THEN 'key'
        WHEN 'photo' THEN 'interest'
    END,
    ROW_NUMBER() OVER (PARTITION BY p.route_id ORDER BY p.created_at ASC, p.id ASC),
    p.name,
    NULLIF(TRIM(p.category), ''),
    NULLIF(TRIM(p.sub_category), ''),
    NULLIF(TRIM(p.description), ''),
    p.latitude,
    p.longitude,
    p.elevation,
    'WGS84'
FROM poi_points p
WHERE p.status = 'confirmed'
  AND p.category IN ('start', 'end', 'camp', 'water', 'supply', 'danger', 'pass', 'valley', 'photo')
  AND NULLIF(TRIM(p.name), '') IS NOT NULL
  AND p.latitude BETWEEN -90 AND 90
  AND p.longitude BETWEEN -180 AND 180;
