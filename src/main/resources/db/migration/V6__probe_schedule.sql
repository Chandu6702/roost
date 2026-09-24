CREATE TABLE probe_schedule (
    id BIGSERIAL PRIMARY KEY,
    monitor_id BIGINT NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    next_probe_at TIMESTAMPTZ NOT NULL
);