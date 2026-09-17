package com.chandu.roost.probe;

import com.chandu.roost.monitor.Monitor;
import com.chandu.roost.monitor.MonitorRepository;
import com.chandu.roost.monitor.MonitorType;
import com.chandu.roost.workspace.Workspace;
import com.chandu.roost.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ProbeResultRepositoryTest {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private ProbeResultRepository probeResultRepository;

    @Test
    void shouldPersistWorkspaceMonitorsAndProbeResults() {

        // 1. Create and persist one workspace
        Workspace workspace =
                workspaceRepository.save(
                        new Workspace(
                                "Production",
                                "production-test-" + System.currentTimeMillis()
                        )
                );

        // 2. Create two monitors belonging to the workspace
        Monitor monitor1 = new Monitor(
                workspace,
                "Google",
                "https://google.com",
                MonitorType.HTTP,
                60,
                5000,
                Instant.now()
        );

        Monitor monitor2 = new Monitor(
                workspace,
                "GitHub",
                "https://github.com",
                MonitorType.HTTP,
                60,
                5000,
                Instant.now()
        );

        monitor1 = monitorRepository.save(monitor1);
        monitor2 = monitorRepository.save(monitor2);

        // 3. Create five probe results
        ProbeResult result1 = new ProbeResult(
                monitor1,
                Instant.now(),
                "UP",
                120,
                null
        );

        ProbeResult result2 = new ProbeResult(
                monitor1,
                Instant.now(),
                "UP",
                130,
                null
        );

        ProbeResult result3 = new ProbeResult(
                monitor1,
                Instant.now(),
                "DOWN",
                5000,
                "Connection timeout"
        );

        ProbeResult result4 = new ProbeResult(
                monitor2,
                Instant.now(),
                "UP",
                90,
                null
        );

        ProbeResult result5 = new ProbeResult(
                monitor2,
                Instant.now(),
                "UP",
                100,
                null
        );

        // 4. Synchronize both sides of the Monitor ↔ ProbeResult relationship
        monitor1.addProbeResult(result1);
        monitor1.addProbeResult(result2);
        monitor1.addProbeResult(result3);

        monitor2.addProbeResult(result4);
        monitor2.addProbeResult(result5);

        // 5. Persist probe results
        probeResultRepository.save(result1);
        probeResultRepository.save(result2);
        probeResultRepository.save(result3);
        probeResultRepository.save(result4);
        probeResultRepository.save(result5);

        // 6. Verify Workspace → Monitor relationship
        assertEquals(workspace.getId(), monitor1.getWorkspace().getId());
        assertEquals(workspace.getId(), monitor2.getWorkspace().getId());

        // 7. Verify Monitor → ProbeResult relationship
        assertEquals(monitor1.getId(), result1.getMonitor().getId());
        assertEquals(monitor1.getId(), result2.getMonitor().getId());
        assertEquals(monitor1.getId(), result3.getMonitor().getId());

        assertEquals(monitor2.getId(), result4.getMonitor().getId());
        assertEquals(monitor2.getId(), result5.getMonitor().getId());
    }
}