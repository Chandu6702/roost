package com.chandu.roost.dto;

public record CreateWorkspaceRequest(
        String name,
        String slug
) {
}