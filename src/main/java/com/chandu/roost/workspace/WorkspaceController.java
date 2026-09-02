package com.chandu.roost.workspace;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspaceController {
    private final WorkspaceService service;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.service = workspaceService;
    }

    @PostMapping("/workspaces")
    public ResponseEntity<Workspace> createWorkspace(@RequestBody CreateWorkspaceRequest request) {
        Workspace obj = service.createWorkspace(request.name());
        return ResponseEntity.status(201).body(obj);
    }
}
