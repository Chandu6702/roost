CREATE INDEX idx_monitors_workspace_id
    ON monitors(workspace_id);

CREATE INDEX idx_probe_results_monitor_probed_at
    ON probe_results(monitor_id, probed_at DESC);