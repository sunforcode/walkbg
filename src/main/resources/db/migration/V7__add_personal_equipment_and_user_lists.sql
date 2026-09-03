CREATE TABLE personal_equipment (
    id varchar(64) NOT NULL,
    name varchar(200) NOT NULL,
    owned_quantity integer NOT NULL,
    unit_weight_grams bigint,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_personal_equipment_quantity CHECK (owned_quantity > 0),
    CONSTRAINT ck_personal_equipment_weight CHECK (unit_weight_grams IS NULL OR unit_weight_grams > 0)
) ENGINE=InnoDB;

CREATE TABLE personal_equipment_ownership (
    personal_equipment_id varchar(64) NOT NULL,
    account_id varchar(64) NOT NULL,
    normalized_name varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (personal_equipment_id),
    CONSTRAINT uk_personal_equipment_owner_name UNIQUE (account_id, normalized_name),
    CONSTRAINT fk_personal_equipment_owner_equipment FOREIGN KEY (personal_equipment_id) REFERENCES personal_equipment (id),
    CONSTRAINT fk_personal_equipment_owner_account FOREIGN KEY (account_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_personal_equipment_owner ON personal_equipment_ownership (account_id);

CREATE TABLE user_equipment_lists (
    id varchar(64) NOT NULL,
    name varchar(200) NOT NULL,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE equipment_list_ownership (
    equipment_list_id varchar(64) NOT NULL,
    account_id varchar(64) NOT NULL,
    PRIMARY KEY (equipment_list_id),
    CONSTRAINT fk_equipment_list_owner_list FOREIGN KEY (equipment_list_id) REFERENCES user_equipment_lists (id),
    CONSTRAINT fk_equipment_list_owner_account FOREIGN KEY (account_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_equipment_list_owner ON equipment_list_ownership (account_id);

CREATE TABLE equipment_list_members (
    equipment_list_id varchar(64) NOT NULL,
    personal_equipment_id varchar(64) NOT NULL,
    quantity integer NOT NULL,
    PRIMARY KEY (equipment_list_id, personal_equipment_id),
    CONSTRAINT ck_equipment_list_member_quantity CHECK (quantity > 0),
    CONSTRAINT fk_equipment_list_member_list FOREIGN KEY (equipment_list_id) REFERENCES user_equipment_lists (id),
    CONSTRAINT fk_equipment_list_member_equipment FOREIGN KEY (personal_equipment_id) REFERENCES personal_equipment (id)
) ENGINE=InnoDB;

CREATE INDEX idx_equipment_list_member_equipment ON equipment_list_members (personal_equipment_id);

-- Legacy equipment rows mix global items, trip lists, templates, brands, images, prices,
-- and nullable creator links. No legacy row set can be migrated while proving the target
-- account ownership, name uniqueness, positive-value, and long-lived-list invariants together,
-- so V7 deliberately starts the target model empty rather than importing ambiguous data.
