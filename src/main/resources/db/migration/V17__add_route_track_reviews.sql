CREATE TABLE route_track_reviews (
    id varchar(64) NOT NULL,
    route_id varchar(64) NOT NULL,
    candidate_id varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    revision bigint NOT NULL,
    expected_revision bigint NOT NULL,
    request_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    decision varchar(16) NOT NULL,
    complete_hiking_range_confirmed boolean NOT NULL,
    reference_system varchar(64) DEFAULT NULL,
    reason varchar(1000) DEFAULT NULL,
    reviewed_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_route_track_review_revision (route_id, revision),
    UNIQUE KEY uk_route_track_review_request (route_id, request_id),
    CONSTRAINT fk_route_track_review_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT ck_route_track_review_revision CHECK (revision > 0 AND expected_revision >= 0),
    CONSTRAINT ck_route_track_review_request CHECK (CHAR_LENGTH(TRIM(request_id)) > 0),
    CONSTRAINT ck_route_track_review_decision CHECK (
        (decision = 'approved' AND complete_hiking_range_confirmed = true
            AND reference_system IS NOT NULL AND CHAR_LENGTH(TRIM(reference_system)) > 0)
        OR
        (decision = 'rejected' AND complete_hiking_range_confirmed = false
            AND reference_system IS NULL AND reason IS NOT NULL AND CHAR_LENGTH(TRIM(reason)) > 0)
    )
) ENGINE=InnoDB;
