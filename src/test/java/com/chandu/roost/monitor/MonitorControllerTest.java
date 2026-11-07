
package com.chandu.roost.monitor;

import com.chandu.roost.workspace.Workspace;
import com.chandu.roost.shared.config.RequestIdFilter;
import com.chandu.roost.publicstatus.TenantResolutionFilter;
import com.chandu.roost.ratelimit.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.cache.CacheManager;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Exclude the app's custom servlet filters from this controller slice: they are
// @Component filters with their own collaborators (repositories, Redis) that a
// web slice doesn't load, and they're irrelevant to controller behaviour here.
@WebMvcTest(controllers = MonitorController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {TenantResolutionFilter.class, RateLimitFilter.class, RequestIdFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class MonitorControllerTest {

    private static final String USER_EMAIL = "user@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitorService monitorService;

    @MockitoBean
    private CacheManager cacheManager;

    @Test
    void getMonitor_whenMonitorExists_returns200() throws Exception {
        Workspace workspace = new Workspace(
                "Test Workspace",
                "test-workspace"
        );

        Monitor monitor = new Monitor(
                workspace,
                "Prod API",
                "https://api.example.com",
                MonitorType.HTTP,
                60,
                5000,
                Instant.now()
        );

        when(monitorService.getMonitor(1L, USER_EMAIL))
                .thenReturn(monitor);

        when(monitorService.getProbeResults(
                eq(1L),
                any(Pageable.class),
                eq(USER_EMAIL)
        )).thenReturn(Page.empty());

        mockMvc.perform(get("/monitors/1")
                        .principal(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Prod API"));
    }

    @Test
    void getMonitor_whenMonitorDoesNotExist_returns404() throws Exception {
        when(monitorService.getMonitor(999L, USER_EMAIL))
                .thenThrow(new MonitorNotFoundException(999L));

        mockMvc.perform(get("/monitors/999")
                        .principal(authenticatedUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createMonitor_whenRequestIsInvalid_returns400() throws Exception {
        String invalidRequest = """
        {
            "workspaceId": null,
            "name": "",
            "url": "",
            "intervalSeconds": 10,
            "timeoutMillis": 50,
            "type": null
        }
        """;

        mockMvc.perform(post("/monitors")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/problem+json"
                ));
    }

    private UsernamePasswordAuthenticationToken authenticatedUser() {
        return new UsernamePasswordAuthenticationToken(
                USER_EMAIL,
                null
        );
    }
}
