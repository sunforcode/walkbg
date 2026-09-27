ALTER TABLE routes
    ADD COLUMN analysis_task_id VARCHAR(64) NULL,
    ADD COLUMN analysis_status VARCHAR(20) NULL,
    ADD COLUMN analysis_previous_status INT NULL,
    ADD COLUMN analysis_started_at DATETIME(6) NULL,
    ADD COLUMN analysis_error TEXT NULL,
    ADD CONSTRAINT uq_routes_analysis_task_id UNIQUE (analysis_task_id);

CREATE TABLE kml_stored_inputs (
    kml_url VARCHAR(500) NOT NULL PRIMARY KEY,
    content LONGTEXT NOT NULL,
    file_size BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL
);
