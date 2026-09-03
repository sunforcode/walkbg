ALTER TABLE personal_trip_equipment_items
    ADD COLUMN normalized_name varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL AFTER name;

-- V8 only creates user-selected list entries (`source = 'user_added'`). Normalize every
-- existing name so an otherwise valid deployment is also safe if unreleased data contains
-- another source. MySQL LOWER is intentionally not used because EquipmentName folds ASCII only.
UPDATE personal_trip_equipment_items
SET normalized_name = REGEXP_REPLACE(
    REGEXP_REPLACE(name, '^[[:space:]]+|[[:space:]]+$', ''),
    '[[:space:]]+',
    ' '
);

UPDATE personal_trip_equipment_items
SET normalized_name = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(CONVERT(normalized_name USING utf8mb4) COLLATE utf8mb4_bin,
        'A','a'),'B','b'),'C','c'),'D','d'),'E','e'),'F','f'),'G','g'),'H','h'),
        'I','i'),'J','j'),'K','k'),'L','l'),'M','m'),'N','n'),'O','o'),'P','p'),
        'Q','q'),'R','r'),'S','s'),'T','t'),'U','u'),'V','v'),'W','w'),'X','x'),
        'Y','y'),'Z','z');

-- Preserve trips that predate the invariant by merging duplicate user-added rows before the
-- database constraint is installed. Existing system-derived rows are never guessed or merged.
UPDATE personal_trip_equipment_items keeper
JOIN (
    SELECT snapshot_id, normalized_name, MIN(display_order) AS keeper_order, SUM(quantity) AS total_quantity
    FROM personal_trip_equipment_items
    WHERE source = 'user_added'
    GROUP BY snapshot_id, normalized_name
    HAVING COUNT(*) > 1
) duplicates
  ON duplicates.snapshot_id = keeper.snapshot_id
 AND duplicates.normalized_name = keeper.normalized_name
 AND duplicates.keeper_order = keeper.display_order
SET keeper.quantity = duplicates.total_quantity;

DELETE duplicate_item
FROM personal_trip_equipment_items duplicate_item
JOIN personal_trip_equipment_items keeper
  ON keeper.snapshot_id = duplicate_item.snapshot_id
 AND keeper.normalized_name = duplicate_item.normalized_name
 AND keeper.source = 'user_added'
 AND duplicate_item.source = 'user_added'
 AND keeper.display_order < duplicate_item.display_order;

ALTER TABLE personal_trip_equipment_items
    MODIFY COLUMN normalized_name varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    ADD CONSTRAINT uk_personal_trip_item_normalized_name UNIQUE (snapshot_id, normalized_name);

CREATE TABLE personal_trip_equipment_item_derivations (
    item_id varchar(64) NOT NULL,
    trip_id varchar(64) NOT NULL,
    logical_suggestion_id varchar(64) NOT NULL,
    suggestion_occurrence_id varchar(64),
    PRIMARY KEY (item_id),
    CONSTRAINT uk_trip_equipment_derivation_logical UNIQUE (trip_id, logical_suggestion_id),
    CONSTRAINT uk_trip_equipment_derivation_item_trip UNIQUE (item_id, trip_id),
    CONSTRAINT fk_trip_equipment_derivation_item FOREIGN KEY (item_id)
        REFERENCES personal_trip_equipment_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_equipment_derivation_trip FOREIGN KEY (trip_id)
        REFERENCES personal_trips (id) ON DELETE CASCADE,
    CONSTRAINT ck_trip_equipment_derivation_logical CHECK (CHAR_LENGTH(TRIM(logical_suggestion_id)) > 0),
    CONSTRAINT ck_trip_equipment_derivation_occurrence CHECK (
        suggestion_occurrence_id IS NULL OR CHAR_LENGTH(TRIM(suggestion_occurrence_id)) > 0
    )
) ENGINE=InnoDB;

CREATE INDEX idx_trip_equipment_derivation_trip
    ON personal_trip_equipment_item_derivations (trip_id);

CREATE TABLE personal_trip_equipment_suppressions (
    trip_id varchar(64) NOT NULL,
    logical_suggestion_id varchar(64) NOT NULL,
    PRIMARY KEY (trip_id, logical_suggestion_id),
    CONSTRAINT fk_trip_equipment_suppression_trip FOREIGN KEY (trip_id)
        REFERENCES personal_trips (id) ON DELETE CASCADE,
    CONSTRAINT ck_trip_equipment_suppression_logical CHECK (CHAR_LENGTH(TRIM(logical_suggestion_id)) > 0)
) ENGINE=InnoDB;
