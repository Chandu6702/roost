package com.chandu.roost.repository;

import com.chandu.roost.entity.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    List<Monitor> findByWorkspaceId(Long workspaceId);

    List<Monitor> findByWorkspaceIdAndNameContainingIgnoreCase(
            Long workspaceId,
            String q
    );
}
