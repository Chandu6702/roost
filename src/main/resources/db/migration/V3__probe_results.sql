CREATE TABLE  probe_results (
    id BIGSERIAL PRIMARY KEY,
    monitor_id BIGINT NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    probed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL,
    latency_ms INT NOT NULL,
    error_message VARCHAR(2048)
)