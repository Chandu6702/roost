-- Monitors are private by default; a tenant opts each one onto its public
-- status page. Keeping the default FALSE means nothing leaks publicly by accident.
ALTER TABLE monitors
    ADD COLUMN published BOOLEAN NOT NULL DEFAULT FALSE;
