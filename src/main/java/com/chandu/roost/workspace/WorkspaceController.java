package com.chandu.roost.workspace;

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
}
