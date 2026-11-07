package com.chandu.roost.monitor;

import org.springframework.data.jpa.domain.Specification;

public class MonitorSpecification {

    public static Specification<Monitor> hasType(MonitorType type) {
        return (root, query, criteriaBuilder) ->
                type == null
                        ? null
                        : criteriaBuilder.equal(root.get("type"), type);
    }

    public static Specification<Monitor> belongsToWorkspace(Long workspaceId) {
        return (root, query, criteriaBuilder) ->
                workspaceId == null
                        ? null
                        : criteriaBuilder.equal(
                        root.get("workspace").get("id"),
                        workspaceId
                );
    }
}
