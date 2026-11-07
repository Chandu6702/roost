package com.chandu.roost.workspace;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces")
public class WorkspaceController {

    private final WorkspaceService service;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.service = workspaceService;
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<Workspace> getWorkspaceBySlug(
            @PathVariable String slug,
            Authentication authentication) {

        return ResponseEntity.ok(
                service.getWorkspaceBySlugForUser(
                        slug, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<Workspace> createWorkspace(
            @RequestBody CreateWorkspaceRequest request,
            Authentication authentication) {

        Workspace workspace = service.createWorkspaceForUser(
                request.name(),
                request.slug(),
                authentication.getName());

        return ResponseEntity.status(201).body(workspace);
    }

    @GetMapping("/with-monitors")
    public ResponseEntity<String> getWorkspacesWithMonitors(
            Authentication authentication) {

        List<Workspace> workspaces =
                service.getCurrentWorkspaceWithMonitors(
                        authentication.getName());

        workspaces.forEach(workspace ->
                workspace.getMonitors().size());

        return ResponseEntity.ok(
                "Current workspace loaded. Check SQL logs.");
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Workspace>> getMyWorkspaces(
            Authentication authentication) {

        List<Workspace> workspaces =
                service.getWorkspacesForUser(authentication.getName());

        return ResponseEntity.ok(workspaces);
    }
}
