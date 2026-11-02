package com.chandu.roost.service;

import com.chandu.roost.entity.User;
import com.chandu.roost.entity.Workspace;
import com.chandu.roost.repository.UserRepository;
import com.chandu.roost.repository.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final CurrentWorkspaceService currentWorkspaceService;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            CurrentWorkspaceService currentWorkspaceService) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.currentWorkspaceService = currentWorkspaceService;
    }

    @Transactional
    public Workspace createWorkspaceForUser(
            String name, String slug, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (user.getWorkspace() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User already owns a workspace");
        }

        Workspace workspace = workspaceRepository.save(
                new Workspace(name, slug));

        user.setWorkspace(workspace);
        userRepository.save(user);

        return workspace;
    }

    @Transactional(readOnly = true)
    public Workspace getWorkspaceBySlugForUser(
            String slug, String email) {

        Workspace workspace = workspaceRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Workspace not found"));

        Long currentWorkspaceId =
                currentWorkspaceService.getCurrentWorkspaceId(email);

        if (!currentWorkspaceId.equals(workspace.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Workspace not found");
        }

        return workspace;
    }

    @Transactional(readOnly = true)
    public List<Workspace> getCurrentWorkspaceWithMonitors(String email) {
        Long workspaceId =
                currentWorkspaceService.getCurrentWorkspaceId(email);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Workspace not found"));

        // Initialize the relationship while the transaction is open.
        workspace.getMonitors().size();

        return List.of(workspace);
    }

    @Transactional(readOnly = true)
    public List<Workspace> getWorkspacesForUser(String email) {
        // A list endpoint: a user with no workspace yet returns [] (so the UI
        // can offer the create-workspace flow), not a 403. Don't route through
        // getCurrentWorkspaceId here — that throws when there's no workspace,
        // which is the right behaviour for tenant resolution but wrong for /mine.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        Workspace workspace = user.getWorkspace();
        return workspace == null ? List.of() : List.of(workspace);
    }
}