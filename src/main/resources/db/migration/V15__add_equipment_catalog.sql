-- Implements define-equipment-catalog and define-equipment-classification-standard.
-- Shared catalog identities are independent of existing personal/trip equipment.
-- spec_type is an internal fixed discriminator, not a public specification fact.
-- Fixed discriminator + composite FK prevents cross-type or cross-table ownership.
CREATE TABLE brand (
    id varchar(64) NOT NULL,
    name varchar(200) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_catalog_brand_id CHECK (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(id, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0),
    CONSTRAINT ck_catalog_brand_name CHECK (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(name, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE equipment (
    id varchar(64) NOT NULL,
    category varchar(40) NOT NULL,
    equipment_type varchar(40) NOT NULL,
    name varchar(200) NOT NULL,
    brand_id varchar(64),
    model varchar(200),
    PRIMARY KEY (id),
    CONSTRAINT uk_catalog_equipment_id_type UNIQUE (id, equipment_type),
    CONSTRAINT fk_catalog_equipment_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE RESTRICT,
    CONSTRAINT ck_catalog_equipment_id CHECK (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(id, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0),
    CONSTRAINT ck_catalog_equipment_name CHECK (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(name, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0),
    CONSTRAINT ck_catalog_equipment_model CHECK (model IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(model, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_catalog_equipment_codes_trimmed CHECK (
        CHAR_LENGTH(category) = CHAR_LENGTH(TRIM(category)) AND
        CHAR_LENGTH(equipment_type) = CHAR_LENGTH(TRIM(equipment_type))
    ),
    CONSTRAINT ck_catalog_equipment_type_category CHECK (
        (category IN ('backpack') AND equipment_type IN ('backpack', 'backpack_rain_cover')) OR
        (category IN ('tent') AND equipment_type IN ('tent', 'groundsheet', 'tent_peg')) OR
        (category IN ('sleeping') AND equipment_type IN ('sleeping_bag', 'sleeping_pad', 'sleeping_quilt', 'pillow', 'sleeping_bag_liner')) OR
        (category IN ('clothing') AND equipment_type IN ('clothing', 'socks', 'hat', 'gloves', 'sunglasses')) OR
        (category IN ('hiking_footwear') AND equipment_type IN ('hiking_shoes')) OR
        (category IN ('walking_aids') AND equipment_type IN ('trekking_pole', 'crampon', 'traction_chain', 'snowshoe', 'river_crossing_shoes')) OR
        (category IN ('camp_essentials') AND equipment_type IN ('camp_shoes', 'camp_chair', 'camp_table', 'sit_pad', 'tarp')) OR
        (category IN ('dining_equipment') AND equipment_type IN ('stove', 'cookware', 'tableware', 'water_bottle', 'hydration_reservoir', 'water_filter')) OR
        (category IN ('clothing', 'dining_equipment') AND equipment_type IN ('storage_bag')) OR
        (category IN ('navigation_communication') AND equipment_type IN ('map', 'compass', 'gps_device', 'two_way_radio', 'satellite_communicator')) OR
        (category IN ('lighting_power') AND equipment_type IN ('headlamp', 'flashlight', 'camp_light', 'power_bank', 'battery', 'charging_cable')) OR
        (category IN ('safety_emergency') AND equipment_type IN ('first_aid_kit', 'whistle', 'emergency_blanket', 'knife', 'repair_tool', 'repair_material')) OR
        (category IN ('hygiene_care') AND equipment_type IN ('toothbrush', 'towel', 'toilet_paper', 'trowel', 'sunscreen', 'insect_repellent', 'menstrual_product')) OR
        (category IN ('miscellaneous') AND equipment_type IN ('waist_pack', 'key', 'document')) OR
        (category IN ('food') AND equipment_type IN ('food')) OR
        (category IN ('drinking_water') AND equipment_type IN ('drinking_water')) OR
        (category IN ('fuel') AND equipment_type IN ('fuel'))
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE INDEX idx_catalog_equipment_category_type ON equipment (category, equipment_type);
CREATE INDEX idx_catalog_equipment_brand ON equipment (brand_id);

CREATE TABLE backpack_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'backpack',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    capacity_liters double,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_backpack_type CHECK (spec_type = 'backpack' AND CHAR_LENGTH(spec_type) = 8),
    CONSTRAINT fk_backpack_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_backpack_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_backpack_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_backpack_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_backpack_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL)),
    CONSTRAINT ck_backpack_capacity_liters CHECK (capacity_liters IS NULL OR (capacity_liters > 0 AND capacity_liters <= 1.7976931348623157e308))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE tent_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'tent',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    capacity_persons integer,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_tent_type CHECK (spec_type = 'tent' AND CHAR_LENGTH(spec_type) = 4),
    CONSTRAINT fk_tent_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_tent_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_tent_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_tent_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_tent_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL)),
    CONSTRAINT ck_tent_complete_unit CHECK (unit_label = '套'),
    CONSTRAINT ck_tent_capacity_persons CHECK (capacity_persons IS NULL OR capacity_persons > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE sleeping_bag_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'sleeping_bag',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    comfort_temperature_celsius double,
    limit_temperature_celsius double,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_sleeping_bag_type CHECK (spec_type = 'sleeping_bag' AND CHAR_LENGTH(spec_type) = 12),
    CONSTRAINT fk_sleeping_bag_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_sleeping_bag_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_sleeping_bag_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_sleeping_bag_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_sleeping_bag_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL)),
    CONSTRAINT ck_sleeping_bag_comfort_temperature_celsius CHECK (comfort_temperature_celsius IS NULL OR (comfort_temperature_celsius >= -1.7976931348623157e308 AND comfort_temperature_celsius <= 1.7976931348623157e308)),
    CONSTRAINT ck_sleeping_bag_limit_temperature_celsius CHECK (limit_temperature_celsius IS NULL OR (limit_temperature_celsius >= -1.7976931348623157e308 AND limit_temperature_celsius <= 1.7976931348623157e308))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE sleeping_pad_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'sleeping_pad',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    pad_type varchar(32),
    r_value double,
    length_cm double,
    max_width_cm double,
    thickness_cm double,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_sleeping_pad_type CHECK (spec_type = 'sleeping_pad' AND CHAR_LENGTH(spec_type) = 12),
    CONSTRAINT fk_sleeping_pad_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_sleeping_pad_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_sleeping_pad_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_sleeping_pad_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_sleeping_pad_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL)),
    CONSTRAINT ck_sleeping_pad_pad_type CHECK (pad_type IS NULL OR (pad_type IN ('closed_cell_foam', 'air', 'self_inflating') AND CHAR_LENGTH(pad_type) = CHAR_LENGTH(TRIM(pad_type)))),
    CONSTRAINT ck_sleeping_pad_r_value CHECK (r_value IS NULL OR (r_value >= 0 AND r_value <= 1.7976931348623157e308)),
    CONSTRAINT ck_sleeping_pad_length_cm CHECK (length_cm IS NULL OR (length_cm > 0 AND length_cm <= 1.7976931348623157e308)),
    CONSTRAINT ck_sleeping_pad_max_width_cm CHECK (max_width_cm IS NULL OR (max_width_cm > 0 AND max_width_cm <= 1.7976931348623157e308)),
    CONSTRAINT ck_sleeping_pad_thickness_cm CHECK (thickness_cm IS NULL OR (thickness_cm > 0 AND thickness_cm <= 1.7976931348623157e308))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE backpack_rain_cover_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'backpack_rain_cover',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_backpack_rain_cover_type CHECK (spec_type = 'backpack_rain_cover' AND CHAR_LENGTH(spec_type) = 19),
    CONSTRAINT fk_backpack_rain_cover_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_backpack_rain_cover_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_backpack_rain_cover_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_backpack_rain_cover_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_backpack_rain_cover_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE groundsheet_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'groundsheet',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_groundsheet_type CHECK (spec_type = 'groundsheet' AND CHAR_LENGTH(spec_type) = 11),
    CONSTRAINT fk_groundsheet_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_groundsheet_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_groundsheet_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_groundsheet_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_groundsheet_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE tent_peg_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'tent_peg',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_tent_peg_type CHECK (spec_type = 'tent_peg' AND CHAR_LENGTH(spec_type) = 8),
    CONSTRAINT fk_tent_peg_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_tent_peg_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_tent_peg_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_tent_peg_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_tent_peg_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE sleeping_quilt_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'sleeping_quilt',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_sleeping_quilt_type CHECK (spec_type = 'sleeping_quilt' AND CHAR_LENGTH(spec_type) = 14),
    CONSTRAINT fk_sleeping_quilt_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_sleeping_quilt_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_sleeping_quilt_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_sleeping_quilt_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_sleeping_quilt_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE pillow_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'pillow',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_pillow_type CHECK (spec_type = 'pillow' AND CHAR_LENGTH(spec_type) = 6),
    CONSTRAINT fk_pillow_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_pillow_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_pillow_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_pillow_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_pillow_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE sleeping_bag_liner_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'sleeping_bag_liner',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_sleeping_bag_liner_type CHECK (spec_type = 'sleeping_bag_liner' AND CHAR_LENGTH(spec_type) = 18),
    CONSTRAINT fk_sleeping_bag_liner_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_sleeping_bag_liner_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_sleeping_bag_liner_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_sleeping_bag_liner_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_sleeping_bag_liner_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE clothing_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'clothing',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_clothing_type CHECK (spec_type = 'clothing' AND CHAR_LENGTH(spec_type) = 8),
    CONSTRAINT fk_clothing_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_clothing_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_clothing_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_clothing_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_clothing_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE socks_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'socks',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_socks_type CHECK (spec_type = 'socks' AND CHAR_LENGTH(spec_type) = 5),
    CONSTRAINT fk_socks_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_socks_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_socks_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_socks_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_socks_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE hat_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'hat',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_hat_type CHECK (spec_type = 'hat' AND CHAR_LENGTH(spec_type) = 3),
    CONSTRAINT fk_hat_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_hat_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_hat_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_hat_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_hat_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE gloves_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'gloves',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_gloves_type CHECK (spec_type = 'gloves' AND CHAR_LENGTH(spec_type) = 6),
    CONSTRAINT fk_gloves_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_gloves_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_gloves_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_gloves_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_gloves_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE sunglasses_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'sunglasses',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_sunglasses_type CHECK (spec_type = 'sunglasses' AND CHAR_LENGTH(spec_type) = 10),
    CONSTRAINT fk_sunglasses_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_sunglasses_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_sunglasses_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_sunglasses_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_sunglasses_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE hiking_shoes_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'hiking_shoes',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_hiking_shoes_type CHECK (spec_type = 'hiking_shoes' AND CHAR_LENGTH(spec_type) = 12),
    CONSTRAINT fk_hiking_shoes_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_hiking_shoes_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_hiking_shoes_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_hiking_shoes_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_hiking_shoes_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE trekking_pole_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'trekking_pole',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_trekking_pole_type CHECK (spec_type = 'trekking_pole' AND CHAR_LENGTH(spec_type) = 13),
    CONSTRAINT fk_trekking_pole_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_trekking_pole_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_trekking_pole_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_trekking_pole_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_trekking_pole_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE crampon_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'crampon',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_crampon_type CHECK (spec_type = 'crampon' AND CHAR_LENGTH(spec_type) = 7),
    CONSTRAINT fk_crampon_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_crampon_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_crampon_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_crampon_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_crampon_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE traction_chain_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'traction_chain',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_traction_chain_type CHECK (spec_type = 'traction_chain' AND CHAR_LENGTH(spec_type) = 14),
    CONSTRAINT fk_traction_chain_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_traction_chain_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_traction_chain_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_traction_chain_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_traction_chain_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE snowshoe_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'snowshoe',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_snowshoe_type CHECK (spec_type = 'snowshoe' AND CHAR_LENGTH(spec_type) = 8),
    CONSTRAINT fk_snowshoe_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_snowshoe_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_snowshoe_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_snowshoe_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_snowshoe_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE river_crossing_shoes_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'river_crossing_shoes',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_river_crossing_shoes_type CHECK (spec_type = 'river_crossing_shoes' AND CHAR_LENGTH(spec_type) = 20),
    CONSTRAINT fk_river_crossing_shoes_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_river_crossing_shoes_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_river_crossing_shoes_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_river_crossing_shoes_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_river_crossing_shoes_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE camp_shoes_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'camp_shoes',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_camp_shoes_type CHECK (spec_type = 'camp_shoes' AND CHAR_LENGTH(spec_type) = 10),
    CONSTRAINT fk_camp_shoes_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_camp_shoes_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_camp_shoes_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_camp_shoes_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_camp_shoes_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE camp_chair_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'camp_chair',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_camp_chair_type CHECK (spec_type = 'camp_chair' AND CHAR_LENGTH(spec_type) = 10),
    CONSTRAINT fk_camp_chair_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_camp_chair_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_camp_chair_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_camp_chair_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_camp_chair_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE camp_table_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'camp_table',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_camp_table_type CHECK (spec_type = 'camp_table' AND CHAR_LENGTH(spec_type) = 10),
    CONSTRAINT fk_camp_table_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_camp_table_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_camp_table_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_camp_table_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_camp_table_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE sit_pad_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'sit_pad',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_sit_pad_type CHECK (spec_type = 'sit_pad' AND CHAR_LENGTH(spec_type) = 7),
    CONSTRAINT fk_sit_pad_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_sit_pad_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_sit_pad_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_sit_pad_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_sit_pad_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE tarp_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'tarp',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_tarp_type CHECK (spec_type = 'tarp' AND CHAR_LENGTH(spec_type) = 4),
    CONSTRAINT fk_tarp_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_tarp_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_tarp_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_tarp_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_tarp_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE stove_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'stove',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_stove_type CHECK (spec_type = 'stove' AND CHAR_LENGTH(spec_type) = 5),
    CONSTRAINT fk_stove_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_stove_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_stove_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_stove_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_stove_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE cookware_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'cookware',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_cookware_type CHECK (spec_type = 'cookware' AND CHAR_LENGTH(spec_type) = 8),
    CONSTRAINT fk_cookware_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_cookware_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_cookware_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_cookware_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_cookware_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE tableware_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'tableware',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_tableware_type CHECK (spec_type = 'tableware' AND CHAR_LENGTH(spec_type) = 9),
    CONSTRAINT fk_tableware_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_tableware_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_tableware_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_tableware_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_tableware_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE water_bottle_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'water_bottle',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_water_bottle_type CHECK (spec_type = 'water_bottle' AND CHAR_LENGTH(spec_type) = 12),
    CONSTRAINT fk_water_bottle_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_water_bottle_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_water_bottle_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_water_bottle_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_water_bottle_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE hydration_reservoir_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'hydration_reservoir',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_hydration_reservoir_type CHECK (spec_type = 'hydration_reservoir' AND CHAR_LENGTH(spec_type) = 19),
    CONSTRAINT fk_hydration_reservoir_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_hydration_reservoir_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_hydration_reservoir_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_hydration_reservoir_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_hydration_reservoir_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE water_filter_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'water_filter',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_water_filter_type CHECK (spec_type = 'water_filter' AND CHAR_LENGTH(spec_type) = 12),
    CONSTRAINT fk_water_filter_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_water_filter_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_water_filter_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_water_filter_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_water_filter_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE storage_bag_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'storage_bag',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_storage_bag_type CHECK (spec_type = 'storage_bag' AND CHAR_LENGTH(spec_type) = 11),
    CONSTRAINT fk_storage_bag_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_storage_bag_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_storage_bag_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_storage_bag_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_storage_bag_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE map_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'map',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_map_type CHECK (spec_type = 'map' AND CHAR_LENGTH(spec_type) = 3),
    CONSTRAINT fk_map_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_map_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_map_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_map_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_map_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE compass_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'compass',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_compass_type CHECK (spec_type = 'compass' AND CHAR_LENGTH(spec_type) = 7),
    CONSTRAINT fk_compass_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_compass_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_compass_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_compass_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_compass_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE gps_device_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'gps_device',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_gps_device_type CHECK (spec_type = 'gps_device' AND CHAR_LENGTH(spec_type) = 10),
    CONSTRAINT fk_gps_device_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_gps_device_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_gps_device_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_gps_device_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_gps_device_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE two_way_radio_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'two_way_radio',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_two_way_radio_type CHECK (spec_type = 'two_way_radio' AND CHAR_LENGTH(spec_type) = 13),
    CONSTRAINT fk_two_way_radio_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_two_way_radio_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_two_way_radio_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_two_way_radio_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_two_way_radio_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE satellite_communicator_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'satellite_communicator',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_satellite_communicator_type CHECK (spec_type = 'satellite_communicator' AND CHAR_LENGTH(spec_type) = 22),
    CONSTRAINT fk_satellite_communicator_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_satellite_communicator_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_satellite_communicator_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_satellite_communicator_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_satellite_communicator_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE headlamp_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'headlamp',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_headlamp_type CHECK (spec_type = 'headlamp' AND CHAR_LENGTH(spec_type) = 8),
    CONSTRAINT fk_headlamp_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_headlamp_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_headlamp_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_headlamp_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_headlamp_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE flashlight_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'flashlight',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_flashlight_type CHECK (spec_type = 'flashlight' AND CHAR_LENGTH(spec_type) = 10),
    CONSTRAINT fk_flashlight_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_flashlight_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_flashlight_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_flashlight_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_flashlight_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE camp_light_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'camp_light',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_camp_light_type CHECK (spec_type = 'camp_light' AND CHAR_LENGTH(spec_type) = 10),
    CONSTRAINT fk_camp_light_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_camp_light_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_camp_light_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_camp_light_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_camp_light_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE power_bank_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'power_bank',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_power_bank_type CHECK (spec_type = 'power_bank' AND CHAR_LENGTH(spec_type) = 10),
    CONSTRAINT fk_power_bank_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_power_bank_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_power_bank_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_power_bank_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_power_bank_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE battery_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'battery',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_battery_type CHECK (spec_type = 'battery' AND CHAR_LENGTH(spec_type) = 7),
    CONSTRAINT fk_battery_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_battery_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_battery_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_battery_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_battery_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE charging_cable_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'charging_cable',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_charging_cable_type CHECK (spec_type = 'charging_cable' AND CHAR_LENGTH(spec_type) = 14),
    CONSTRAINT fk_charging_cable_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_charging_cable_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_charging_cable_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_charging_cable_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_charging_cable_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE first_aid_kit_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'first_aid_kit',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_first_aid_kit_type CHECK (spec_type = 'first_aid_kit' AND CHAR_LENGTH(spec_type) = 13),
    CONSTRAINT fk_first_aid_kit_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_first_aid_kit_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_first_aid_kit_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_first_aid_kit_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_first_aid_kit_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE whistle_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'whistle',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_whistle_type CHECK (spec_type = 'whistle' AND CHAR_LENGTH(spec_type) = 7),
    CONSTRAINT fk_whistle_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_whistle_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_whistle_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_whistle_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_whistle_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE emergency_blanket_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'emergency_blanket',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_emergency_blanket_type CHECK (spec_type = 'emergency_blanket' AND CHAR_LENGTH(spec_type) = 17),
    CONSTRAINT fk_emergency_blanket_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_emergency_blanket_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_emergency_blanket_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_emergency_blanket_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_emergency_blanket_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE knife_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'knife',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_knife_type CHECK (spec_type = 'knife' AND CHAR_LENGTH(spec_type) = 5),
    CONSTRAINT fk_knife_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_knife_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_knife_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_knife_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_knife_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE repair_tool_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'repair_tool',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_repair_tool_type CHECK (spec_type = 'repair_tool' AND CHAR_LENGTH(spec_type) = 11),
    CONSTRAINT fk_repair_tool_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_repair_tool_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_repair_tool_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_repair_tool_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_repair_tool_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE repair_material_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'repair_material',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_repair_material_type CHECK (spec_type = 'repair_material' AND CHAR_LENGTH(spec_type) = 15),
    CONSTRAINT fk_repair_material_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_repair_material_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_repair_material_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_repair_material_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_repair_material_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE toothbrush_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'toothbrush',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_toothbrush_type CHECK (spec_type = 'toothbrush' AND CHAR_LENGTH(spec_type) = 10),
    CONSTRAINT fk_toothbrush_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_toothbrush_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_toothbrush_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_toothbrush_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_toothbrush_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE towel_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'towel',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_towel_type CHECK (spec_type = 'towel' AND CHAR_LENGTH(spec_type) = 5),
    CONSTRAINT fk_towel_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_towel_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_towel_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_towel_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_towel_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE toilet_paper_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'toilet_paper',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_toilet_paper_type CHECK (spec_type = 'toilet_paper' AND CHAR_LENGTH(spec_type) = 12),
    CONSTRAINT fk_toilet_paper_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_toilet_paper_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_toilet_paper_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_toilet_paper_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_toilet_paper_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE trowel_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'trowel',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_trowel_type CHECK (spec_type = 'trowel' AND CHAR_LENGTH(spec_type) = 6),
    CONSTRAINT fk_trowel_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_trowel_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_trowel_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_trowel_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_trowel_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE sunscreen_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'sunscreen',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_sunscreen_type CHECK (spec_type = 'sunscreen' AND CHAR_LENGTH(spec_type) = 9),
    CONSTRAINT fk_sunscreen_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_sunscreen_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_sunscreen_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_sunscreen_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_sunscreen_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE insect_repellent_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'insect_repellent',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_insect_repellent_type CHECK (spec_type = 'insect_repellent' AND CHAR_LENGTH(spec_type) = 16),
    CONSTRAINT fk_insect_repellent_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_insect_repellent_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_insect_repellent_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_insect_repellent_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_insect_repellent_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE menstrual_product_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'menstrual_product',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_menstrual_product_type CHECK (spec_type = 'menstrual_product' AND CHAR_LENGTH(spec_type) = 17),
    CONSTRAINT fk_menstrual_product_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_menstrual_product_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_menstrual_product_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_menstrual_product_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_menstrual_product_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE waist_pack_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'waist_pack',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_waist_pack_type CHECK (spec_type = 'waist_pack' AND CHAR_LENGTH(spec_type) = 10),
    CONSTRAINT fk_waist_pack_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_waist_pack_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_waist_pack_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_waist_pack_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_waist_pack_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE key_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'key',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_key_type CHECK (spec_type = 'key' AND CHAR_LENGTH(spec_type) = 3),
    CONSTRAINT fk_key_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_key_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_key_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_key_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_key_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE document_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'document',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_document_type CHECK (spec_type = 'document' AND CHAR_LENGTH(spec_type) = 8),
    CONSTRAINT fk_document_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_document_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_document_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_document_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_document_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE food_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'food',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_food_type CHECK (spec_type = 'food' AND CHAR_LENGTH(spec_type) = 4),
    CONSTRAINT fk_food_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_food_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_food_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_food_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_food_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE drinking_water_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'drinking_water',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_drinking_water_type CHECK (spec_type = 'drinking_water' AND CHAR_LENGTH(spec_type) = 14),
    CONSTRAINT fk_drinking_water_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_drinking_water_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_drinking_water_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_drinking_water_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_drinking_water_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE fuel_specs (
    equipment_id varchar(64) NOT NULL,
    spec_type varchar(40) NOT NULL DEFAULT 'fuel',
    variant text,
    unit_label varchar(8) NOT NULL,
    unit_weight_grams double,
    weight_scope text,
    PRIMARY KEY (equipment_id),
    CONSTRAINT ck_fuel_type CHECK (spec_type = 'fuel' AND CHAR_LENGTH(spec_type) = 4),
    CONSTRAINT fk_fuel_equipment FOREIGN KEY (equipment_id, spec_type) REFERENCES equipment (id, equipment_type) ON DELETE CASCADE,
    CONSTRAINT ck_fuel_unit CHECK (unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份') AND CHAR_LENGTH(unit_label) = 1),
    CONSTRAINT ck_fuel_weight CHECK (unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)),
    CONSTRAINT ck_fuel_variant CHECK (variant IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(variant, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)),
    CONSTRAINT ck_fuel_scope CHECK ((weight_scope IS NULL OR (CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(weight_scope, CHAR(9), ''), CHAR(10), ''), CHAR(13), ''))) > 0)) AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
