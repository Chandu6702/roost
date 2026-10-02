package com.chandu.roost.integration;

import com.chandu.roost.dto.CreateWorkspaceRequest;
import com.chandu.roost.dto.MonitorRequest;
import com.chandu.roost.dto.MonitorResponse;
import com.chandu.roost.dto.ProbeResultRequest;
import com.chandu.roost.dto.ProbeResultResponse;
import com.chandu.roost.entity.MonitorType;
import com.chandu.roost.entity.Workspace;
import com.chandu.roost.repository.AuditLogRepository;
import com.chandu.roost.repository.ProbeResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class MonitorPersistenceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProbeResultRepository probeResultRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void createWorkspace_returns201() {

        // 1. Create workspace
        CreateWorkspaceRequest workspaceRequest =
                new CreateWorkspaceRequest(
                        "Integration Test Workspace",
                        "integration-test"
                );

        ResponseEntity<Workspace> workspaceResponse =
                restTemplate.postForEntity(
                        "http://localhost:" + port + "/workspaces",
                        workspaceRequest,
                        Workspace.class
                );

        assertEquals(201, workspaceResponse.getStatusCode().value());
        assertNotNull(workspaceResponse.getBody());
        assertEquals(
                "Integration Test Workspace",
                workspaceResponse.getBody().getName()
        );

        Long workspaceId = workspaceResponse.getBody().getId();

        assertNotNull(workspaceId);

        // 2. Create monitor
        MonitorRequest monitorRequest =
                new MonitorRequest(
                        workspaceId,
                        "Google Monitor",
                        "https://google.com",
                        60,
                        5000,
                        MonitorType.HTTP
                );

        ResponseEntity<MonitorResponse> monitorResponse =
                restTemplate.postForEntity(
                        "http://localhost:" + port + "/monitors",
                        monitorRequest,
                        MonitorResponse.class
                );

        assertEquals(201, monitorResponse.getStatusCode().value());
        assertNotNull(monitorResponse.getBody());
        assertNotNull(monitorResponse.getBody().id());
        assertEquals(
                "Google Monitor",
                monitorResponse.getBody().name()
        );

        Long monitorId = monitorResponse.getBody().id();

        // 3. Record one probe result
        ProbeResultRequest probeRequest =
                new ProbeResultRequest(
                        Instant.now(),
                        "UP",
                        120,
                        null
                );

        ResponseEntity<ProbeResultResponse> probeResponse =
                restTemplate.postForEntity(
                        "http://localhost:" + port
                                + "/monitors/" + monitorId
                                + "/probe-results",
                        probeRequest,
                        ProbeResultResponse.class
                );

        assertEquals(201, probeResponse.getStatusCode().value());
        assertNotNull(probeResponse.getBody());
        assertNotNull(probeResponse.getBody().id());
        assertEquals(
                "UP",
                probeResponse.getBody().status()
        );
        assertEquals(
                120,
                probeResponse.getBody().latencyMs()
        );

        // 4. Read the monitor back
        ResponseEntity<String> getResponse =
                restTemplate.getForEntity(
                        "http://localhost:" + port
                                + "/monitors/" + monitorId,
                        String.class
                );

        assertEquals(200, getResponse.getStatusCode().value());
        assertNotNull(getResponse.getBody());

        System.out.println(getResponse.getBody());
    }

    @Test
    void concurrentProbeWrites_persistAllResults() throws Exception {

        // 1. Create a workspace
        CreateWorkspaceRequest workspaceRequest =
                new CreateWorkspaceRequest(
                        "Concurrent Probe Workspace",
                        "concurrent-probe-test"
                );

        ResponseEntity<Workspace> workspaceResponse =
                restTemplate.postForEntity(
                        "http://localhost:" + port + "/workspaces",
                        workspaceRequest,
                        Workspace.class
                );

        assertEquals(201, workspaceResponse.getStatusCode().value());
        assertNotNull(workspaceResponse.getBody());

        Long workspaceId = workspaceResponse.getBody().getId();

        // 2. Create a monitor
        MonitorRequest monitorRequest =
                new MonitorRequest(
                        workspaceId,
                        "Concurrent Probe Monitor",
                        "https://google.com",
                        60,
                        5000,
                        MonitorType.HTTP
                );

        ResponseEntity<MonitorResponse> monitorResponse =
                restTemplate.postForEntity(
                        "http://localhost:" + port + "/monitors",
                        monitorRequest,
                        MonitorResponse.class
                );

        assertEquals(201, monitorResponse.getStatusCode().value());
        assertNotNull(monitorResponse.getBody());

        Long monitorId = monitorResponse.getBody().id();

        // 3. Prepare 20 concurrent requests
        int threadCount = 20;

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);

        CountDownLatch ready =
                new CountDownLatch(threadCount);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<ResponseEntity<ProbeResultResponse>>> futures =
                new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {

            final int probeNumber = i;

            Future<ResponseEntity<ProbeResultResponse>> future =
                    executor.submit(() -> {

                        ready.countDown();

                        // Wait until all 20 threads are ready
                        start.await();

                        ProbeResultRequest probeRequest =
                                new ProbeResultRequest(
                                        Instant.now().plusMillis(probeNumber),
                                        "UP",
                                        100 + probeNumber,
                                        null
                                );

                        return restTemplate.postForEntity(
                                "http://localhost:" + port
                                        + "/monitors/" + monitorId
                                        + "/probe-results",
                                probeRequest,
                                ProbeResultResponse.class
                        );
                    });

            futures.add(future);
        }

        // 4. Make sure all 20 threads are ready
        assertTrue(
                ready.await(10, TimeUnit.SECONDS),
                "Not all probe threads became ready"
        );

        // 5. Release all 20 threads together
        start.countDown();

        // 6. Wait for all HTTP requests to complete
        for (Future<ResponseEntity<ProbeResultResponse>> future : futures) {

            ResponseEntity<ProbeResultResponse> response =
                    future.get();

            assertEquals(201, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().id());
        }

        executor.shutdown();

        assertTrue(
                executor.awaitTermination(30, TimeUnit.SECONDS),
                "Probe executor did not terminate"
        );

        // 7. Verify that all 20 probe results were persisted
        long probeResultCount =
                probeResultRepository.countByMonitorId(monitorId);

        assertEquals(
                20,
                probeResultCount,
                "Expected exactly 20 probe results"
        );

        // 8. Verify that every probe result produced an audit entry
        long auditCount =
                auditLogRepository.countByMonitorIdAndAction(
                        monitorId,
                        "PROBE_RESULT_RECORDED"
                );

        assertEquals(
                20,
                auditCount,
                "Expected exactly 20 probe-result audit entries"
        );
    }
}