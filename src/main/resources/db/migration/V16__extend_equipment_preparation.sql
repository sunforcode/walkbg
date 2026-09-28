-- extend-equipment-preparation-api: nullable additions preserve unknown legacy facts.
-- UnitWeight has always allowed finite fractional grams; do not truncate adopted catalog weights.
ALTER TABLE personal_equipment
    MODIFY COLUMN unit_weight_grams DOUBLE NULL,
    ADD COLUMN category varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    ADD COLUMN equipment_type varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    ADD COLUMN variant TEXT NULL,
    ADD COLUMN unit_label varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    ADD COLUMN weight_scope TEXT NULL,
    ADD COLUMN unit_weight_source varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    ADD COLUMN catalog_equipment_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    ADD CONSTRAINT fk_personal_equipment_catalog FOREIGN KEY (catalog_equipment_id)
        REFERENCES equipment (id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_pe_finite_weight CHECK (
        unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)
    ),
    ADD CONSTRAINT ck_pe_weight_source CHECK (
        unit_weight_source IS NULL OR (
            unit_weight_grams IS NOT NULL
            AND unit_weight_source IN ('catalog_reference', 'user_recorded')
            AND (unit_weight_source <> 'catalog_reference' OR (
                catalog_equipment_id IS NOT NULL AND unit_label IS NOT NULL AND weight_scope IS NOT NULL
            ))
        )
    ),
    ADD CONSTRAINT ck_pe_category CHECK (category IS NULL OR category IN ('backpack', 'tent', 'sleeping', 'clothing', 'hiking_footwear', 'walking_aids', 'camp_essentials', 'dining_equipment', 'navigation_communication', 'lighting_power', 'safety_emergency', 'hygiene_care', 'miscellaneous', 'food', 'drinking_water', 'fuel')),
    ADD CONSTRAINT ck_pe_type_category CHECK (
        equipment_type IS NULL OR (category IS NOT NULL AND (
            (equipment_type = 'backpack' AND category IN ('backpack'))
            OR (equipment_type = 'tent' AND category IN ('tent'))
            OR (equipment_type = 'sleeping_bag' AND category IN ('sleeping'))
            OR (equipment_type = 'sleeping_pad' AND category IN ('sleeping'))
            OR (equipment_type = 'backpack_rain_cover' AND category IN ('backpack'))
            OR (equipment_type = 'groundsheet' AND category IN ('tent'))
            OR (equipment_type = 'tent_peg' AND category IN ('tent'))
            OR (equipment_type = 'sleeping_quilt' AND category IN ('sleeping'))
            OR (equipment_type = 'pillow' AND category IN ('sleeping'))
            OR (equipment_type = 'sleeping_bag_liner' AND category IN ('sleeping'))
            OR (equipment_type = 'clothing' AND category IN ('clothing'))
            OR (equipment_type = 'socks' AND category IN ('clothing'))
            OR (equipment_type = 'hat' AND category IN ('clothing'))
            OR (equipment_type = 'gloves' AND category IN ('clothing'))
            OR (equipment_type = 'sunglasses' AND category IN ('clothing'))
            OR (equipment_type = 'hiking_shoes' AND category IN ('hiking_footwear'))
            OR (equipment_type = 'trekking_pole' AND category IN ('walking_aids'))
            OR (equipment_type = 'crampon' AND category IN ('walking_aids'))
            OR (equipment_type = 'traction_chain' AND category IN ('walking_aids'))
            OR (equipment_type = 'snowshoe' AND category IN ('walking_aids'))
            OR (equipment_type = 'river_crossing_shoes' AND category IN ('walking_aids'))
            OR (equipment_type = 'camp_shoes' AND category IN ('camp_essentials'))
            OR (equipment_type = 'camp_chair' AND category IN ('camp_essentials'))
            OR (equipment_type = 'camp_table' AND category IN ('camp_essentials'))
            OR (equipment_type = 'sit_pad' AND category IN ('camp_essentials'))
            OR (equipment_type = 'tarp' AND category IN ('camp_essentials'))
            OR (equipment_type = 'stove' AND category IN ('dining_equipment'))
            OR (equipment_type = 'cookware' AND category IN ('dining_equipment'))
            OR (equipment_type = 'tableware' AND category IN ('dining_equipment'))
            OR (equipment_type = 'water_bottle' AND category IN ('dining_equipment'))
            OR (equipment_type = 'hydration_reservoir' AND category IN ('dining_equipment'))
            OR (equipment_type = 'water_filter' AND category IN ('dining_equipment'))
            OR (equipment_type = 'storage_bag' AND category IN ('clothing', 'dining_equipment'))
            OR (equipment_type = 'map' AND category IN ('navigation_communication'))
            OR (equipment_type = 'compass' AND category IN ('navigation_communication'))
            OR (equipment_type = 'gps_device' AND category IN ('navigation_communication'))
            OR (equipment_type = 'two_way_radio' AND category IN ('navigation_communication'))
            OR (equipment_type = 'satellite_communicator' AND category IN ('navigation_communication'))
            OR (equipment_type = 'headlamp' AND category IN ('lighting_power'))
            OR (equipment_type = 'flashlight' AND category IN ('lighting_power'))
            OR (equipment_type = 'camp_light' AND category IN ('lighting_power'))
            OR (equipment_type = 'power_bank' AND category IN ('lighting_power'))
            OR (equipment_type = 'battery' AND category IN ('lighting_power'))
            OR (equipment_type = 'charging_cable' AND category IN ('lighting_power'))
            OR (equipment_type = 'first_aid_kit' AND category IN ('safety_emergency'))
            OR (equipment_type = 'whistle' AND category IN ('safety_emergency'))
            OR (equipment_type = 'emergency_blanket' AND category IN ('safety_emergency'))
            OR (equipment_type = 'knife' AND category IN ('safety_emergency'))
            OR (equipment_type = 'repair_tool' AND category IN ('safety_emergency'))
            OR (equipment_type = 'repair_material' AND category IN ('safety_emergency'))
            OR (equipment_type = 'toothbrush' AND category IN ('hygiene_care'))
            OR (equipment_type = 'towel' AND category IN ('hygiene_care'))
            OR (equipment_type = 'toilet_paper' AND category IN ('hygiene_care'))
            OR (equipment_type = 'trowel' AND category IN ('hygiene_care'))
            OR (equipment_type = 'sunscreen' AND category IN ('hygiene_care'))
            OR (equipment_type = 'insect_repellent' AND category IN ('hygiene_care'))
            OR (equipment_type = 'menstrual_product' AND category IN ('hygiene_care'))
            OR (equipment_type = 'waist_pack' AND category IN ('miscellaneous'))
            OR (equipment_type = 'key' AND category IN ('miscellaneous'))
            OR (equipment_type = 'document' AND category IN ('miscellaneous'))
            OR (equipment_type = 'food' AND category IN ('food'))
            OR (equipment_type = 'drinking_water' AND category IN ('drinking_water'))
            OR (equipment_type = 'fuel' AND category IN ('fuel'))
        ))
    ),
    ADD CONSTRAINT ck_pe_variant CHECK (variant IS NULL OR CHAR_LENGTH(TRIM(variant)) > 0),
    ADD CONSTRAINT ck_pe_unit CHECK (unit_label IS NULL OR unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份')),
    ADD CONSTRAINT ck_pe_scope CHECK (weight_scope IS NULL OR CHAR_LENGTH(TRIM(weight_scope)) > 0);

ALTER TABLE personal_trip_equipment_items
    MODIFY COLUMN unit_weight_grams DOUBLE NULL,
    ADD COLUMN category varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    ADD COLUMN equipment_type varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    ADD COLUMN variant TEXT NULL,
    ADD COLUMN unit_label varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    ADD COLUMN weight_scope TEXT NULL,
    ADD COLUMN backpack_quantity integer NULL,
    ADD COLUMN worn_quantity integer NULL,
    ADD COLUMN handheld_quantity integer NULL,
    ADD COLUMN supply_kind varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    ADD COLUMN supply_amount DOUBLE NULL,
    ADD COLUMN supply_amount_unit varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    ADD COLUMN supply_container_mode varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    ADD COLUMN packed_quantity integer NULL,
    ADD CONSTRAINT ck_tpei_finite_weight CHECK (
        unit_weight_grams IS NULL OR (unit_weight_grams > 0 AND unit_weight_grams <= 1.7976931348623157e308)
    ),
    ADD CONSTRAINT ck_tpei_category CHECK (category IS NULL OR category IN ('backpack', 'tent', 'sleeping', 'clothing', 'hiking_footwear', 'walking_aids', 'camp_essentials', 'dining_equipment', 'navigation_communication', 'lighting_power', 'safety_emergency', 'hygiene_care', 'miscellaneous', 'food', 'drinking_water', 'fuel')),
    ADD CONSTRAINT ck_tpei_type_category CHECK (
        equipment_type IS NULL OR (category IS NOT NULL AND (
            (equipment_type = 'backpack' AND category IN ('backpack'))
            OR (equipment_type = 'tent' AND category IN ('tent'))
            OR (equipment_type = 'sleeping_bag' AND category IN ('sleeping'))
            OR (equipment_type = 'sleeping_pad' AND category IN ('sleeping'))
            OR (equipment_type = 'backpack_rain_cover' AND category IN ('backpack'))
            OR (equipment_type = 'groundsheet' AND category IN ('tent'))
            OR (equipment_type = 'tent_peg' AND category IN ('tent'))
            OR (equipment_type = 'sleeping_quilt' AND category IN ('sleeping'))
            OR (equipment_type = 'pillow' AND category IN ('sleeping'))
            OR (equipment_type = 'sleeping_bag_liner' AND category IN ('sleeping'))
            OR (equipment_type = 'clothing' AND category IN ('clothing'))
            OR (equipment_type = 'socks' AND category IN ('clothing'))
            OR (equipment_type = 'hat' AND category IN ('clothing'))
            OR (equipment_type = 'gloves' AND category IN ('clothing'))
            OR (equipment_type = 'sunglasses' AND category IN ('clothing'))
            OR (equipment_type = 'hiking_shoes' AND category IN ('hiking_footwear'))
            OR (equipment_type = 'trekking_pole' AND category IN ('walking_aids'))
            OR (equipment_type = 'crampon' AND category IN ('walking_aids'))
            OR (equipment_type = 'traction_chain' AND category IN ('walking_aids'))
            OR (equipment_type = 'snowshoe' AND category IN ('walking_aids'))
            OR (equipment_type = 'river_crossing_shoes' AND category IN ('walking_aids'))
            OR (equipment_type = 'camp_shoes' AND category IN ('camp_essentials'))
            OR (equipment_type = 'camp_chair' AND category IN ('camp_essentials'))
            OR (equipment_type = 'camp_table' AND category IN ('camp_essentials'))
            OR (equipment_type = 'sit_pad' AND category IN ('camp_essentials'))
            OR (equipment_type = 'tarp' AND category IN ('camp_essentials'))
            OR (equipment_type = 'stove' AND category IN ('dining_equipment'))
            OR (equipment_type = 'cookware' AND category IN ('dining_equipment'))
            OR (equipment_type = 'tableware' AND category IN ('dining_equipment'))
            OR (equipment_type = 'water_bottle' AND category IN ('dining_equipment'))
            OR (equipment_type = 'hydration_reservoir' AND category IN ('dining_equipment'))
            OR (equipment_type = 'water_filter' AND category IN ('dining_equipment'))
            OR (equipment_type = 'storage_bag' AND category IN ('clothing', 'dining_equipment'))
            OR (equipment_type = 'map' AND category IN ('navigation_communication'))
            OR (equipment_type = 'compass' AND category IN ('navigation_communication'))
            OR (equipment_type = 'gps_device' AND category IN ('navigation_communication'))
            OR (equipment_type = 'two_way_radio' AND category IN ('navigation_communication'))
            OR (equipment_type = 'satellite_communicator' AND category IN ('navigation_communication'))
            OR (equipment_type = 'headlamp' AND category IN ('lighting_power'))
            OR (equipment_type = 'flashlight' AND category IN ('lighting_power'))
            OR (equipment_type = 'camp_light' AND category IN ('lighting_power'))
            OR (equipment_type = 'power_bank' AND category IN ('lighting_power'))
            OR (equipment_type = 'battery' AND category IN ('lighting_power'))
            OR (equipment_type = 'charging_cable' AND category IN ('lighting_power'))
            OR (equipment_type = 'first_aid_kit' AND category IN ('safety_emergency'))
            OR (equipment_type = 'whistle' AND category IN ('safety_emergency'))
            OR (equipment_type = 'emergency_blanket' AND category IN ('safety_emergency'))
            OR (equipment_type = 'knife' AND category IN ('safety_emergency'))
            OR (equipment_type = 'repair_tool' AND category IN ('safety_emergency'))
            OR (equipment_type = 'repair_material' AND category IN ('safety_emergency'))
            OR (equipment_type = 'toothbrush' AND category IN ('hygiene_care'))
            OR (equipment_type = 'towel' AND category IN ('hygiene_care'))
            OR (equipment_type = 'toilet_paper' AND category IN ('hygiene_care'))
            OR (equipment_type = 'trowel' AND category IN ('hygiene_care'))
            OR (equipment_type = 'sunscreen' AND category IN ('hygiene_care'))
            OR (equipment_type = 'insect_repellent' AND category IN ('hygiene_care'))
            OR (equipment_type = 'menstrual_product' AND category IN ('hygiene_care'))
            OR (equipment_type = 'waist_pack' AND category IN ('miscellaneous'))
            OR (equipment_type = 'key' AND category IN ('miscellaneous'))
            OR (equipment_type = 'document' AND category IN ('miscellaneous'))
            OR (equipment_type = 'food' AND category IN ('food'))
            OR (equipment_type = 'drinking_water' AND category IN ('drinking_water'))
            OR (equipment_type = 'fuel' AND category IN ('fuel'))
        ))
    ),
    ADD CONSTRAINT ck_tpei_variant CHECK (variant IS NULL OR CHAR_LENGTH(TRIM(variant)) > 0),
    ADD CONSTRAINT ck_tpei_unit CHECK (unit_label IS NULL OR unit_label IN ('件', '只', '根', '对', '双', '套', '包', '瓶', '罐', '份')),
    ADD CONSTRAINT ck_tpei_scope CHECK (weight_scope IS NULL OR CHAR_LENGTH(TRIM(weight_scope)) > 0),
    ADD CONSTRAINT ck_tpei_carry CHECK (
        (backpack_quantity IS NULL AND worn_quantity IS NULL AND handheld_quantity IS NULL)
        OR (
            backpack_quantity IS NOT NULL AND worn_quantity IS NOT NULL AND handheld_quantity IS NOT NULL
            AND backpack_quantity >= 0 AND worn_quantity >= 0 AND handheld_quantity >= 0
            AND backpack_quantity + worn_quantity + handheld_quantity = quantity
        )
    ),
    ADD CONSTRAINT ck_tpei_packed CHECK (
        packed_quantity IS NULL OR (packed_quantity >= 0 AND packed_quantity <= COALESCE(backpack_quantity, 0))
    ),
    ADD CONSTRAINT ck_tpei_supply CHECK (
        (supply_kind IS NULL AND supply_amount IS NULL AND supply_amount_unit IS NULL AND supply_container_mode IS NULL
            AND (category IS NULL OR category NOT IN ('food', 'drinking_water', 'fuel')))
        OR (
            supply_kind IS NOT NULL AND supply_amount IS NOT NULL
            AND supply_amount_unit IS NOT NULL AND supply_container_mode IS NOT NULL
            AND category IS NOT NULL AND equipment_type IS NOT NULL AND unit_label IS NOT NULL
            AND supply_kind IN ('food', 'drinking_water', 'fuel')
            AND category = supply_kind AND equipment_type = supply_kind
            AND quantity = 1 AND unit_label = '份'
            AND supply_amount > 0 AND supply_amount <= 1.7976931348623157e308
            AND ((supply_kind = 'food' AND supply_amount_unit IN ('gram', 'portion'))
                OR (supply_kind = 'drinking_water' AND supply_amount_unit = 'milliliter')
                OR (supply_kind = 'fuel' AND supply_amount_unit = 'gram'))
            AND supply_container_mode IN ('included', 'excluded')
            AND (unit_weight_grams IS NULL OR weight_scope IS NOT NULL)
            AND (backpack_quantity IS NULL OR (backpack_quantity = 1 AND worn_quantity = 0 AND handheld_quantity = 0))
            AND ownership_status = 'unconfirmed_owned'
        )
    );

ALTER TABLE personal_trip_equipment_snapshots
    MODIFY COLUMN known_total_weight_grams DOUBLE NOT NULL,
    ADD CONSTRAINT ck_tpes_finite_weight CHECK (
        known_total_weight_grams >= 0 AND known_total_weight_grams <= 1.7976931348623157e308
    );

-- Account row locking serializes first execution and replay in the same business transaction.
-- Hash the unbounded opaque key instead of imposing a new public key length limit.
CREATE TABLE equipment_command_receipts (
    account_id varchar(64) NOT NULL,
    operation_key varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    idempotency_key_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    response_json LONGTEXT NOT NULL,
    created_at datetime(6) NOT NULL,
    PRIMARY KEY (account_id, operation_key, idempotency_key_hash),
    CONSTRAINT fk_equipment_receipt_account FOREIGN KEY (account_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_equipment_receipt_operation CHECK (CHAR_LENGTH(TRIM(operation_key)) > 0),
    CONSTRAINT ck_equipment_receipt_json CHECK (JSON_VALID(response_json))
) ENGINE=InnoDB;
