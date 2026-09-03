CREATE TABLE personal_trips (
    id varchar(64) NOT NULL,
    name varchar(200) NOT NULL,
    first_generated_at datetime(6) NOT NULL,
    lifecycle_state varchar(32) NOT NULL,
    departure_city varchar(200) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    total_day_count integer NOT NULL,
    hiking_day_count integer NOT NULL,
    revision varchar(64) NOT NULL,
    frozen_route_basis_json LONGTEXT NOT NULL,
    weather_overview_json LONGTEXT,
    important_notices_json LONGTEXT,
    selected_transport_option_id varchar(64),
    updated_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_personal_trip_lifecycle CHECK (lifecycle_state IN ('active', 'cancelled')),
    CONSTRAINT ck_personal_trip_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_personal_trip_day_counts CHECK (
        total_day_count > 0 AND hiking_day_count > 0 AND hiking_day_count <= total_day_count
    )
) ENGINE=InnoDB;

CREATE INDEX idx_personal_trips_start_date ON personal_trips (start_date);
CREATE INDEX idx_personal_trips_end_date ON personal_trips (end_date);

CREATE TABLE personal_trip_ownership (
    trip_id varchar(64) NOT NULL,
    account_id varchar(64) NOT NULL,
    PRIMARY KEY (trip_id),
    CONSTRAINT fk_personal_trip_owner_trip FOREIGN KEY (trip_id) REFERENCES personal_trips (id),
    CONSTRAINT fk_personal_trip_owner_account FOREIGN KEY (account_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_personal_trip_owner ON personal_trip_ownership (account_id);

CREATE TABLE trip_frozen_route_versions (
    trip_id varchar(64) NOT NULL,
    route_version_id varchar(64) NOT NULL,
    PRIMARY KEY (trip_id),
    CONSTRAINT fk_trip_frozen_version_trip FOREIGN KEY (trip_id) REFERENCES personal_trips (id),
    CONSTRAINT fk_trip_frozen_version_route FOREIGN KEY (route_version_id) REFERENCES route_versions (id)
) ENGINE=InnoDB;

CREATE INDEX idx_trip_frozen_version_route ON trip_frozen_route_versions (route_version_id);

CREATE TABLE personal_trip_days (
    id varchar(64) NOT NULL,
    trip_id varchar(64) NOT NULL,
    day_number integer NOT NULL,
    date date NOT NULL,
    primary_stage varchar(100) NOT NULL,
    hiking_day_number integer,
    content_json LONGTEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_personal_trip_day_number UNIQUE (trip_id, day_number),
    CONSTRAINT fk_personal_trip_day_trip FOREIGN KEY (trip_id) REFERENCES personal_trips (id),
    CONSTRAINT ck_personal_trip_day_number CHECK (day_number > 0),
    CONSTRAINT ck_personal_trip_hiking_day_number CHECK (hiking_day_number IS NULL OR hiking_day_number > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_personal_trip_days_trip ON personal_trip_days (trip_id);

CREATE TABLE personal_trip_equipment_snapshots (
    id varchar(64) NOT NULL,
    trip_id varchar(64) NOT NULL,
    item_count integer NOT NULL,
    known_total_weight_grams bigint NOT NULL,
    missing_weight_item_count integer NOT NULL,
    owned_item_count integer NOT NULL,
    unconfirmed_owned_item_count integer NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_personal_trip_snapshot_trip UNIQUE (trip_id),
    CONSTRAINT fk_personal_trip_snapshot_trip FOREIGN KEY (trip_id) REFERENCES personal_trips (id),
    CONSTRAINT ck_personal_trip_snapshot_counts CHECK (
        item_count >= 0
        AND known_total_weight_grams >= 0
        AND missing_weight_item_count >= 0
        AND owned_item_count >= 0
        AND unconfirmed_owned_item_count >= 0
        AND owned_item_count + unconfirmed_owned_item_count = item_count
        AND missing_weight_item_count <= item_count
    )
) ENGINE=InnoDB;

CREATE TABLE personal_trip_equipment_items (
    id varchar(64) NOT NULL,
    snapshot_id varchar(64) NOT NULL,
    display_order integer NOT NULL,
    name varchar(200) NOT NULL,
    quantity integer NOT NULL,
    unit_weight_grams bigint,
    source varchar(32) NOT NULL,
    ownership_status varchar(32) NOT NULL,
    note TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uk_personal_trip_item_order UNIQUE (snapshot_id, display_order),
    CONSTRAINT fk_personal_trip_item_snapshot FOREIGN KEY (snapshot_id) REFERENCES personal_trip_equipment_snapshots (id),
    CONSTRAINT ck_personal_trip_item_order CHECK (display_order > 0),
    CONSTRAINT ck_personal_trip_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_personal_trip_item_weight CHECK (unit_weight_grams IS NULL OR unit_weight_grams > 0),
    CONSTRAINT ck_personal_trip_item_source CHECK (
        source IN ('system_suggestion', 'user_added', 'user_adjusted')
    ),
    CONSTRAINT ck_personal_trip_item_ownership CHECK (
        ownership_status IN ('owned', 'unconfirmed_owned')
    )
) ENGINE=InnoDB;

CREATE INDEX idx_personal_trip_equipment_items_snapshot ON personal_trip_equipment_items (snapshot_id);

CREATE TABLE personal_trip_idempotency (
    id varchar(64) NOT NULL,
    account_id varchar(64) NOT NULL,
    operation_name varchar(64) NOT NULL,
    idempotency_key varchar(200) NOT NULL,
    request_hash varchar(64) NOT NULL,
    response_type varchar(100) NOT NULL,
    response_json LONGTEXT NOT NULL,
    created_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_personal_trip_idempotency UNIQUE (account_id, operation_name, idempotency_key),
    CONSTRAINT fk_personal_trip_idempotency_account FOREIGN KEY (account_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE trip_transport_selections (
    selection_id varchar(64) NOT NULL,
    account_id varchar(64) NOT NULL,
    request_hash varchar(64) NOT NULL,
    context_json LONGTEXT NOT NULL,
    options_json LONGTEXT NOT NULL,
    created_at datetime(6) NOT NULL,
    PRIMARY KEY (selection_id),
    CONSTRAINT fk_trip_transport_selection_account FOREIGN KEY (account_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_trip_transport_selection_account ON trip_transport_selections (account_id);

-- Legacy trips do not contain the complete frozen route basis, ordered actual days, aggregate
-- revision, or independent current equipment snapshot required by the personal-trip contract.
-- V8 therefore starts the target aggregate empty rather than importing unverifiable legacy rows.
