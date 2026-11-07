package com.chandu.roost.publicstatus;

import com.chandu.roost.workspace.Workspace;
import com.chandu.roost.workspace.WorkspaceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Resolves the tenant for public status-page requests from the {@code Host}
 * header — {@code <slug>.roost.local} → the workspace with that slug. The
 * resolved id/name are stashed as request attributes for
 * {@link PublicStatusController}. This is the second tenant-resolution path
 * (the authenticated path resolves tenant from the JWT instead).
 *
 * <p>Only runs for {@code /public/} requests; everything else passes straight
 * through to the normal security chain.
 */
@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    static final String WORKSPACE_ID_ATTR = "roost.publicWorkspaceId";
    static final String WORKSPACE_NAME_ATTR = "roost.publicWorkspaceName";

    private static final String TENANT_SUFFIX = ".roost.local";

    private final WorkspaceRepository workspaceRepository;

    public TenantResolutionFilter(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/public/");
    }

    // No @Transactional here: a servlet filter annotated transactional gets a
    // CGLIB proxy, and GenericFilterBean.init() is final and cannot be proxied,
    // so the container fails to start the filter. The single findBySlug call is
    // already transactional (Spring Data), and only scalar fields are read.
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        extractSlug(request.getHeader("Host"))
                .flatMap(workspaceRepository::findBySlug)
                .ifPresent(ws -> bind(request, ws));

        filterChain.doFilter(request, response);
    }

    private void bind(HttpServletRequest request, Workspace ws) {
        request.setAttribute(WORKSPACE_ID_ATTR, ws.getId());
        request.setAttribute(WORKSPACE_NAME_ATTR, ws.getName());
    }

    /** {@code acme.roost.local:8080} → {@code "acme"}. */
    private Optional<String> extractSlug(String host) {
        if (host == null) {
            return Optional.empty();
        }
        String hostname = host.split(":", 2)[0].toLowerCase();
        if (!hostname.endsWith(TENANT_SUFFIX)) {
            return Optional.empty();
        }
        String slug = hostname.substring(
                0, hostname.length() - TENANT_SUFFIX.length());
        return slug.isBlank() ? Optional.empty() : Optional.of(slug);
    }
}
