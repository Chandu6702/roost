package com.chandu.roost.publicstatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Public status", description = "Unauthenticated per-tenant status page")
@RestController
@RequestMapping("/public/status")
public class PublicStatusController {

    private final PublicStatusService publicStatusService;

    public PublicStatusController(PublicStatusService publicStatusService) {
        this.publicStatusService = publicStatusService;
    }

    @Operation(summary = "Public status for the tenant resolved from the Host header")
    @GetMapping
    public PublicStatusResponse status(HttpServletRequest request) {
        Long workspaceId =
                (Long) request.getAttribute(TenantResolutionFilter.WORKSPACE_ID_ATTR);
        String workspaceName =
                (String) request.getAttribute(TenantResolutionFilter.WORKSPACE_NAME_ATTR);

        if (workspaceId == null) {
            // No tenant resolved from the Host header — treat as unknown page.
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Unknown status page");
        }

        return publicStatusService.build(workspaceId, workspaceName);
    }
}
