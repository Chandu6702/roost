package com.chandu.roost.repository;

import com.chandu.roost.entity.Workspace;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    Optional<Workspace> findBySlug(String slug);

    @EntityGraph(attributePaths = "monitors")
    @Query("select w from Workspace w")
    List<Workspace> findAllWithMonitorsUsingGraph();
}