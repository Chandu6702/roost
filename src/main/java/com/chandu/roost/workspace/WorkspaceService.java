package com.chandu.roost.workspace;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    public Workspace createWorkspace(String name, String slug) {
        Workspace workspace = new Workspace(name, slug);
        return workspaceRepository.save(workspace);
    }

    public Workspace getWorkspaceBySlug(String slug) {
        return workspaceRepository.findBySlug(slug)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Workspace with slug " + slug + " was not found"
                        ));
    }

    @Transactional
    public List<Workspace> getWorkspacesWithMonitors() {

        List<Workspace> workspaces =
                workspaceRepository.findAllWithMonitorsUsingGraph();

        for (Workspace workspace : workspaces) {
            workspace.getMonitors().size();
        }

        return workspaces;
    }
}