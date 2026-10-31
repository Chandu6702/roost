-- Optimistic-lock version + JPA audit columns for incidents.
ALTER TABLE incidents
    ADD COLUMN version    BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN created_at TIMESTAMPTZ,
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN created_by VARCHAR(255),
    ADD COLUMN updated_by VARCHAR(255);

-- Backfill audit timestamps for rows that predate auditing.
UPDATE incidents
SET created_at = opened_at,
    updated_at = COALESCE(resolved_at, acknowledged_at, opened_at),
    created_by = 'system',
    updated_by = 'system'
WHERE created_at IS NULL;
