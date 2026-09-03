ALTER TABLE route_versions
    ADD CONSTRAINT uk_route_version_id_route UNIQUE (id, route_id);

CREATE TABLE route_version_publication_order (
    route_id varchar(64) NOT NULL,
    route_version_id varchar(64) NOT NULL,
    published_sequence integer NOT NULL,
    PRIMARY KEY (route_version_id),
    CONSTRAINT uk_route_version_publication_sequence UNIQUE (route_id, published_sequence),
    CONSTRAINT uk_route_version_publication_version UNIQUE (route_id, route_version_id),
    CONSTRAINT fk_route_version_publication_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT fk_route_version_publication_version FOREIGN KEY (route_version_id, route_id)
        REFERENCES route_versions (id, route_id),
    CONSTRAINT ck_route_version_publication_sequence CHECK (published_sequence > 0)
) ENGINE=InnoDB;

-- Preserve the explicit current-public relation as the existing maximum publication fact.
-- Any other pre-existing version has no reliable ordering evidence and is intentionally left
-- without a publication sequence; labels, identities, timestamps and storage order are not used.
INSERT INTO route_version_publication_order (route_id, route_version_id, published_sequence)
SELECT current.route_id, current.route_version_id, 1
FROM route_current_public_versions current;

ALTER TABLE route_current_public_versions
    ADD CONSTRAINT fk_current_public_version_publication FOREIGN KEY (route_id, route_version_id)
        REFERENCES route_version_publication_order (route_id, route_version_id);

CREATE TABLE logical_equipment_suggestion_identities (
    route_id varchar(64) NOT NULL,
    logical_suggestion_id varchar(64) NOT NULL,
    PRIMARY KEY (route_id, logical_suggestion_id),
    CONSTRAINT fk_logical_equipment_suggestion_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT ck_logical_equipment_suggestion_id CHECK (CHAR_LENGTH(TRIM(logical_suggestion_id)) > 0)
) ENGINE=InnoDB;

CREATE TABLE route_version_equipment_suggestions (
    id varchar(64) NOT NULL,
    route_id varchar(64) NOT NULL,
    route_version_id varchar(64) NOT NULL,
    logical_suggestion_id varchar(64) NOT NULL,
    display_order integer NOT NULL,
    name varchar(200) NOT NULL,
    normalized_name varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    quantity integer NOT NULL,
    unit_weight_grams bigint,
    note varchar(500),
    level varchar(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_route_version_suggestion_id_scope UNIQUE (route_version_id, id),
    CONSTRAINT uk_route_version_suggestion_order UNIQUE (route_version_id, display_order),
    CONSTRAINT uk_route_version_suggestion_name UNIQUE (route_version_id, normalized_name),
    CONSTRAINT uk_route_version_suggestion_logical UNIQUE (route_version_id, logical_suggestion_id),
    CONSTRAINT fk_route_version_suggestion_version_scope FOREIGN KEY (route_version_id, route_id)
        REFERENCES route_versions (id, route_id),
    CONSTRAINT fk_route_version_suggestion_logical_scope FOREIGN KEY (route_id, logical_suggestion_id)
        REFERENCES logical_equipment_suggestion_identities (route_id, logical_suggestion_id),
    CONSTRAINT ck_route_version_suggestion_id CHECK (CHAR_LENGTH(TRIM(id)) > 0),
    CONSTRAINT ck_route_version_suggestion_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_route_version_suggestion_normalized_name CHECK (CHAR_LENGTH(TRIM(normalized_name)) > 0),
    CONSTRAINT ck_route_version_suggestion_quantity CHECK (quantity > 0),
    CONSTRAINT ck_route_version_suggestion_weight CHECK (unit_weight_grams IS NULL OR unit_weight_grams > 0),
    CONSTRAINT ck_route_version_suggestion_note CHECK (note IS NULL OR CHAR_LENGTH(TRIM(note)) > 0),
    CONSTRAINT ck_route_version_suggestion_level CHECK (level IN ('required', 'recommended'))
) ENGINE=InnoDB;

CREATE INDEX idx_route_version_suggestion_logical
    ON route_version_equipment_suggestions (route_id, logical_suggestion_id);

ALTER TABLE personal_trip_equipment_item_derivations
    ADD COLUMN route_id varchar(64) NULL AFTER trip_id;

UPDATE personal_trip_equipment_item_derivations derivation
JOIN trip_frozen_route_versions frozen ON frozen.trip_id = derivation.trip_id
JOIN route_versions version ON version.id = frozen.route_version_id
SET derivation.route_id = version.route_id;

INSERT INTO logical_equipment_suggestion_identities (route_id, logical_suggestion_id)
SELECT DISTINCT route_id, logical_suggestion_id
FROM personal_trip_equipment_item_derivations;

-- V9 could persist an occurrence label before the occurrence aggregate existed. It is not safe to
-- infer which new V10 occurrence it meant, so retain the explicit logical identity and clear only
-- that optional unverified occurrence edge.
UPDATE personal_trip_equipment_item_derivations
SET suggestion_occurrence_id = NULL
WHERE suggestion_occurrence_id IS NOT NULL;

ALTER TABLE personal_trip_equipment_item_derivations
    MODIFY COLUMN route_id varchar(64) NOT NULL,
    ADD CONSTRAINT fk_trip_equipment_derivation_logical FOREIGN KEY (route_id, logical_suggestion_id)
        REFERENCES logical_equipment_suggestion_identities (route_id, logical_suggestion_id),
    ADD CONSTRAINT fk_trip_equipment_derivation_occurrence FOREIGN KEY (suggestion_occurrence_id)
        REFERENCES route_version_equipment_suggestions (id);

ALTER TABLE personal_trip_equipment_suppressions
    ADD COLUMN route_id varchar(64) NULL AFTER trip_id;

UPDATE personal_trip_equipment_suppressions suppression
JOIN trip_frozen_route_versions frozen ON frozen.trip_id = suppression.trip_id
JOIN route_versions version ON version.id = frozen.route_version_id
SET suppression.route_id = version.route_id;

INSERT INTO logical_equipment_suggestion_identities (route_id, logical_suggestion_id)
SELECT DISTINCT suppression.route_id, suppression.logical_suggestion_id
FROM personal_trip_equipment_suppressions suppression
LEFT JOIN logical_equipment_suggestion_identities logical
  ON logical.route_id = suppression.route_id
 AND logical.logical_suggestion_id = suppression.logical_suggestion_id
WHERE logical.logical_suggestion_id IS NULL;

ALTER TABLE personal_trip_equipment_suppressions
    MODIFY COLUMN route_id varchar(64) NOT NULL,
    ADD CONSTRAINT fk_trip_equipment_suppression_logical FOREIGN KEY (route_id, logical_suggestion_id)
        REFERENCES logical_equipment_suggestion_identities (route_id, logical_suggestion_id);
