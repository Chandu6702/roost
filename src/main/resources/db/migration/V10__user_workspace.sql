ALTER TABLE users
    ADD COLUMN workspace_id BIGINT
        REFERENCES workspaces(id);

CREATE UNIQUE INDEX uq_users_workspace_id
    ON users(workspace_id);