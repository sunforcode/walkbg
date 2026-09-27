CREATE TABLE route_publication_configuration (
    id integer NOT NULL,
    last_all_route_order integer NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_route_publication_configuration_singleton CHECK (id = 1),
    CONSTRAINT ck_route_publication_configuration_order CHECK (last_all_route_order >= 0)
) ENGINE=InnoDB;

INSERT INTO route_publication_configuration (id, last_all_route_order)
SELECT 1, COALESCE(MAX(all_route_order), 0) FROM public_route_collection;

CREATE TABLE route_publication_requests (
    route_id varchar(64) NOT NULL,
    publication_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    route_version_id varchar(64) NOT NULL,
    public_route_type varchar(32) NOT NULL,
    PRIMARY KEY (route_id, publication_id),
    CONSTRAINT fk_route_publication_request_version FOREIGN KEY (route_id, route_version_id)
        REFERENCES route_version_publication_order (route_id, route_version_id),
    CONSTRAINT ck_route_publication_request_identity CHECK (CHAR_LENGTH(TRIM(publication_id)) > 0),
    CONSTRAINT ck_route_publication_request_type CHECK (public_route_type IN ('one_day', 'multi_day'))
) ENGINE=InnoDB;
