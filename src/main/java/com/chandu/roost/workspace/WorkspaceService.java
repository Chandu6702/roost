package com.chandu.roost.workspace;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WorkspaceService {

    public Workspace createWorkspace(String name) {
        String id = UUID.randomUUID().toString();
        return new Workspace(id, name);
    }
}
