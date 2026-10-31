package com.chandu.roost.repository;

import com.chandu.roost.entity.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface MonitorRepository
        extends JpaRepository<Monitor, Long>,
        JpaSpecificationExecutor<Monitor> {

    Optional<Monitor> findByIdAndWorkspaceId(
            Long id, Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    List<Monitor> findByWorkspaceId(Long workspaceId);

    List<Monitor> findByWorkspaceIdAndPublishedTrue(Long workspaceId);

    List<Monitor> findByWorkspaceIdAndNameContainingIgnoreCase(
            Long workspaceId, String name);
}