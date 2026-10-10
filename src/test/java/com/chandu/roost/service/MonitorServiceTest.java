package com.chandu.roost.service;

import com.chandu.roost.dto.MonitorRequest;
import com.chandu.roost.entity.Monitor;
import com.chandu.roost.entity.MonitorType;
import com.chandu.roost.entity.Workspace;
import com.chandu.roost.error.MonitorNotFoundException;
import com.chandu.roost.repository.MonitorRepository;
import com.chandu.roost.repository.ProbeResultRepository;
import com.chandu.roost.repository.ProbeScheduleRepository;
import com.chandu.roost.repository.WorkspaceRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    private static final String USER_EMAIL = "user@example.com";
    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private ProbeResultRepository probeResultRepository;

    @Mock
    private ProbeScheduleRepository probeScheduleRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private CurrentWorkspaceService currentWorkspaceService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Filter workspaceFilter;

    private MonitorService monitorService;

    @BeforeEach
    void setUp() {
        monitorService = new MonitorService(
                monitorRepository,
                workspaceRepository,
                probeResultRepository,
                probeScheduleRepository,
                auditService,
                currentWorkspaceService,
                entityManager
        );

        when(currentWorkspaceService.getCurrentWorkspaceId(USER_EMAIL))
                .thenReturn(WORKSPACE_ID);
    }

    private void mockWorkspaceFilter() {
        doReturn(session)
                .when(entityManager)
                .unwrap(Session.class);

        when(session.getEnabledFilter("workspaceFilter"))
                .thenReturn(null);

        when(session.enableFilter("workspaceFilter"))
                .thenReturn(workspaceFilter);

        when(workspaceFilter.setParameter("workspaceId", WORKSPACE_ID))
                .thenReturn(workspaceFilter);
    }

    @Test
    void getMonitor_whenMonitorExists_returnsMonitor() {
        mockWorkspaceFilter();

        Monitor expectedMonitor = new Monitor(
                null,
                "Google",
                "https://google.com",
                MonitorType.HTTP,
                60,
                5000,
                Instant.now()
        );

        when(monitorRepository.findByIdAndWorkspaceId(
                1L, WORKSPACE_ID
        )).thenReturn(Optional.of(expectedMonitor));

        Monitor result = monitorService.getMonitor(1L, USER_EMAIL);

        assertSame(expectedMonitor, result);

        verify(monitorRepository).findByIdAndWorkspaceId(
                1L, WORKSPACE_ID
        );
    }

    @Test
    void createMonitor_savesMonitor() {
        mockWorkspaceFilter();

        Workspace workspace = new Workspace(
                "Test Workspace",
                "test-workspace"
        );

        when(workspaceRepository.findById(WORKSPACE_ID))
                .thenReturn(Optional.of(workspace));

        MonitorRequest request = new MonitorRequest(
                WORKSPACE_ID,
                "Google Monitor",
                "https://google.com",
                60,
                5000,
                MonitorType.HTTP
        );

        when(monitorRepository.save(any(Monitor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Monitor result = monitorService.createMonitor(
                request, USER_EMAIL
        );

        assertSame(workspace, result.getWorkspace());

        verify(monitorRepository).save(any(Monitor.class));
    }

    @Test
    void getMonitor_whenMonitorDoesNotExist_throwsMonitorNotFoundException() {
        mockWorkspaceFilter();

        when(monitorRepository.findByIdAndWorkspaceId(
                999L, WORKSPACE_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.getMonitor(999L, USER_EMAIL)
        );
    }
}
