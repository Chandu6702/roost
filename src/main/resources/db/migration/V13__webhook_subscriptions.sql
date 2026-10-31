-- Per-tenant webhook subscribers. Each workspace registers URLs that receive
-- signed incident events. The secret is per-subscription (HMAC key).
CREATE TABLE webhook_subscriptions (
    id           BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces (id),
    url          VARCHAR(2048) NOT NULL,
    secret       VARCHAR(255)  NOT NULL,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Fan-out looks up active subscriptions by tenant on every incident event.
CREATE INDEX idx_webhook_subscriptions_workspace
    ON webhook_subscriptions (workspace_id)
    WHERE active;
