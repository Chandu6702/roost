ALTER TABLE audit_logs
    ALTER COLUMN incident_id DROP NOT NULL;

ALTER TABLE audit_logs
    ADD COLUMN monitor_id BIGINT
        REFERENCES monitors(id)
            ON DELETE CASCADE;