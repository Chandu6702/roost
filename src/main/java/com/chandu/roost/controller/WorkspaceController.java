package com.chandu.roost.controller;

import com.chandu.roost.dto.CreateWorkspaceRequest;
import com.chandu.roost.entity.Workspace;
import com.chandu.roost.service.WorkspaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/workspaces")
public class WorkspaceController {
    private final WorkspaceService service;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.service = workspaceService;
    }
    @GetMapping("/slug/{slug}")
    public ResponseEntity<Workspace> getWorkspaceBySlug(
            @PathVariable String slug) {

        return ResponseEntity.ok(
                service.getWorkspaceBySlug(slug)
        );
    }
    @PostMapping
    public ResponseEntity<Workspace> createWorkspace(
            @RequestBody CreateWorkspaceRequest request) {

        Workspace workspace = service.createWorkspace(
                request.name(),
                request.slug()
        );

        return ResponseEntity.status(201).body(workspace);
    }

    @GetMapping("/with-monitors")
    public ResponseEntity<String> getWorkspacesWithMonitors() {
        service.getWorkspacesWithMonitors();
        return ResponseEntity.ok("N+1 query executed. Check SQL logs.");
    }
}
