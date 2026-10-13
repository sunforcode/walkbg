CREATE TABLE scene_media (
    media_id varchar(64) NOT NULL,
    content_type varchar(32) NOT NULL,
    width integer NOT NULL,
    height integer NOT NULL,
    media_bytes LONGBLOB NOT NULL,
    PRIMARY KEY (media_id),
    CONSTRAINT ck_scene_media_dimensions CHECK (width > 0 AND height > 0)
) ENGINE=InnoDB;

CREATE TABLE route_scene_sets (
    route_version_id varchar(64) NOT NULL,
    route_id varchar(64) NOT NULL,
    revision bigint NOT NULL,
    content_json LONGTEXT NOT NULL,
    PRIMARY KEY (route_version_id),
    CONSTRAINT fk_route_scene_version FOREIGN KEY (route_version_id) REFERENCES route_versions (id),
    CONSTRAINT ck_route_scene_revision CHECK (revision > 0)
) ENGINE=InnoDB;

CREATE TABLE trip_scene_sets (
    trip_id varchar(64) NOT NULL,
    route_version_id varchar(64) NOT NULL,
    trip_revision varchar(64) NOT NULL,
    revision bigint NOT NULL,
    content_json LONGTEXT NOT NULL,
    PRIMARY KEY (trip_id),
    CONSTRAINT fk_trip_scene_trip FOREIGN KEY (trip_id) REFERENCES personal_trips (id),
    CONSTRAINT fk_trip_scene_version FOREIGN KEY (route_version_id) REFERENCES route_versions (id),
    CONSTRAINT ck_trip_scene_revision CHECK (revision > 0)
) ENGINE=InnoDB;
