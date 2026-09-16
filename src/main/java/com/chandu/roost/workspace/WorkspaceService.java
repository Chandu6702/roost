package com.chandu.roost.workspace;

import org.springframework.stereotype.Service;

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
}