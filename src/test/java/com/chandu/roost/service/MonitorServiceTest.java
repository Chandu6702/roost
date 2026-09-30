package com.chandu.roost.service;

import com.chandu.roost.dto.MonitorRequest;
import com.chandu.roost.entity.Workspace;
import com.chandu.roost.error.MonitorNotFoundException;
import com.chandu.roost.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.chandu.roost.entity.Monitor;
import com.chandu.roost.entity.MonitorType;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

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

    @InjectMocks
    private MonitorService monitorService;

    @Test
    void getMonitor_whenMonitorExists_returnsMonitor() {
        Monitor mockMonitor = new Monitor(
                null,
                "Google",
                "https://google.com",
                MonitorType.HTTP,
                60,
                5000,
                Instant.now()
        );

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(mockMonitor));

        Monitor result = monitorService.getMonitor(1L);

        assertSame(mockMonitor, result);
    }

    @Test
    void createMonitor_savesMonitor() {
        Workspace workspace = new Workspace(
                "Test Workspace",
                "test-workspace"
        );

        when(workspaceRepository.findById(1L))
                .thenReturn(Optional.of(workspace));

        MonitorRequest request = new MonitorRequest(
                1L,
                "Google Monitor",
                "https://google.com",
                60,
                5000,
                MonitorType.HTTP
        );

        when(monitorRepository.save(any(Monitor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Monitor result = monitorService.createMonitor(request);

        verify(monitorRepository).save(any(Monitor.class));
    }

    @Test
    void getMonitor_whenMonitorDoesNotExist_throwsMonitorNotFoundException() {
        when(monitorRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.getMonitor(999L)
        );
    }
}