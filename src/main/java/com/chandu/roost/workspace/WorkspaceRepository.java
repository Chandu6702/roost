package com.chandu.roost.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace,Long> {
    Optional<Workspace> findBySlug(String slug);
}
