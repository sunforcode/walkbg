-- Add the soft-delete column required by the deployed Route entity.
-- Existing routes predate soft deletion, so preserve them as active.

ALTER TABLE routes
    ADD COLUMN is_deleted bit(1) NOT NULL DEFAULT b'0';

CREATE INDEX idx_routes_is_deleted ON routes (is_deleted);
