CREATE TABLE incidents
(
    id BIGSERIAL PRIMARY KEY ,
    monitor_id BIGINT  NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    acknowledged_at TIMESTAMP WITH TIME ZONE ,
    resolved_at TIMESTAMP WITH TIME ZONE,
    summary TEXT
)