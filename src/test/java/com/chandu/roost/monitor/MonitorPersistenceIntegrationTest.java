
package com.chandu.roost.monitor;

import com.chandu.roost.workspace.CreateWorkspaceRequest;
import com.chandu.roost.identity.LoginRequest;
import com.chandu.roost.identity.LoginResponse;
import com.chandu.roost.probe.ProbeResultRequest;
import com.chandu.roost.probe.ProbeResultResponse;
import com.chandu.roost.identity.RegisterRequest;
import com.chandu.roost.workspace.Workspace;
import com.chandu.roost.audit.AuditLogRepository;
import com.chandu.roost.probe.ProbeResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
class MonitorPersistenceIntegrationTest {

    private static final String PASSWORD = "IntegrationTestPassword123!";

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProbeResultRepository probeResultRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void createWorkspace_returns201() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        HttpHeaders headers = authenticate("workspace-" + uniqueId + "@example.com");

        Workspace workspace = createWorkspace(
                headers,
                "Integration Test Workspace",
                "integration-" + uniqueId
        );

        assertNotNull(workspace.getId());
        assertEquals("Integration Test Workspace", workspace.getName());

        MonitorResponse monitor = createMonitor(
                headers,
                workspace.getId(),
                "Google Monitor"
        );

        assertNotNull(monitor.id());
        assertEquals("Google Monitor", monitor.name());

        ProbeResultRequest probeRequest = new ProbeResultRequest(
                Instant.now(),
                "UP",
                120,
                null
        );

        ResponseEntity<ProbeResultResponse> probeResponse =
                restTemplate.exchange(
                        url("/monitors/" + monitor.id() + "/probe-results"),
                        HttpMethod.POST,
                        new HttpEntity<>(probeRequest, headers),
                        ProbeResultResponse.class
                );

        assertEquals(201, probeResponse.getStatusCode().value());
        assertNotNull(probeResponse.getBody());
        assertNotNull(probeResponse.getBody().id());
        assertEquals("UP", probeResponse.getBody().status());
        assertEquals(120, probeResponse.getBody().latencyMs());

        ResponseEntity<String> getResponse = restTemplate.exchange(
                url("/monitors/" + monitor.id()),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(200, getResponse.getStatusCode().value());
        assertNotNull(getResponse.getBody());
    }

    @Test
    void concurrentProbeWrites_persistAllResults() throws Exception {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        HttpHeaders headers = authenticate("concurrent-" + uniqueId + "@example.com");

        Workspace workspace = createWorkspace(
                headers,
                "Concurrent Probe Workspace",
                "concurrent-" + uniqueId
        );

        MonitorResponse monitor = createMonitor(
                headers,
                workspace.getId(),
                "Concurrent Probe Monitor"
        );

        Long monitorId = monitor.id();
        int threadCount = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<ResponseEntity<ProbeResultResponse>>> futures =
                new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                final int probeNumber = i;

                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Timed out waiting to start probe request"
                        );
                    }

                    ProbeResultRequest probeRequest = new ProbeResultRequest(
                            Instant.now().plusMillis(probeNumber),
                            "UP",
                            100 + probeNumber,
                            null
                    );

                    return restTemplate.exchange(
                            url("/monitors/" + monitorId + "/probe-results"),
                            HttpMethod.POST,
                            new HttpEntity<>(probeRequest, headers),
                            ProbeResultResponse.class
                    );
                }));
            }

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Not all probe threads became ready"
            );

            start.countDown();

            for (Future<ResponseEntity<ProbeResultResponse>> future : futures) {
                ResponseEntity<ProbeResultResponse> response =
                        future.get(30, TimeUnit.SECONDS);

                assertEquals(201, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertNotNull(response.getBody().id());
            }
        } finally {
            start.countDown();
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(30, TimeUnit.SECONDS),
                    "Probe executor did not terminate"
            );
        }

        assertEquals(
                20,
                probeResultRepository.countByMonitorId(monitorId),
                "Expected exactly 20 probe results"
        );

        assertEquals(
                20,
                auditLogRepository.countByMonitorIdAndAction(
                        monitorId,
                        "PROBE_RESULT_RECORDED"
                ),
                "Expected exactly 20 probe-result audit entries"
        );
    }


    @Test
    @Transactional
    void workspaceFilter_onlyReturnsMonitorsForSelectedWorkspace() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Create two independent users, workspaces, and monitors.
        HttpHeaders headersA = authenticate("filter-a-" + uniqueId + "@example.com");
        Workspace workspaceA = createWorkspace(
                headersA, "Filter Workspace A", "filter-a-" + uniqueId
        );
        MonitorResponse monitorA = createMonitor(
                headersA, workspaceA.getId(), "Filter Monitor A"
        );

        HttpHeaders headersB = authenticate("filter-b-" + uniqueId + "@example.com");
        Workspace workspaceB = createWorkspace(
                headersB, "Filter Workspace B", "filter-b-" + uniqueId
        );
        MonitorResponse monitorB = createMonitor(
                headersB, workspaceB.getId(), "Filter Monitor B"
        );

        // Query through the same Hibernate session with the filter enabled.
        entityManager.clear();

        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("workspaceFilter")
                .setParameter("workspaceId", workspaceA.getId());

        List<Long> visibleMonitorIdsA = entityManager
                .createQuery("select m from Monitor m", Monitor.class)
                .getResultList()
                .stream()
                .map(Monitor::getId)
                .sorted(Comparator.naturalOrder())
                .toList();

        assertEquals(
                List.of(monitorA.id()),
                visibleMonitorIdsA,
                "Workspace A's filter should hide Workspace B's monitor"
        );

        // Change only the filter parameter and query again.
        entityManager.clear();

        session.getEnabledFilter("workspaceFilter")
                .setParameter("workspaceId", workspaceB.getId());

        List<Long> visibleMonitorIdsB = entityManager
                .createQuery("select m from Monitor m", Monitor.class)
                .getResultList()
                .stream()
                .map(Monitor::getId)
                .sorted(Comparator.naturalOrder())
                .toList();

        assertEquals(
                List.of(monitorB.id()),
                visibleMonitorIdsB,
                "Workspace B's filter should hide Workspace A's monitor"
        );
    }

    private HttpHeaders authenticate(String email) {
        RegisterRequest registerRequest =
                new RegisterRequest(email, PASSWORD);

        ResponseEntity<Void> registerResponse =
                restTemplate.postForEntity(
                        url("/auth/register"),
                        registerRequest,
                        Void.class
                );

        assertTrue(
                registerResponse.getStatusCode().is2xxSuccessful(),
                "Registration failed for " + email
        );

        LoginRequest loginRequest = new LoginRequest(email, PASSWORD);

        ResponseEntity<LoginResponse> loginResponse =
                restTemplate.postForEntity(
                        url("/auth/login"),
                        loginRequest,
                        LoginResponse.class
                );

        assertEquals(200, loginResponse.getStatusCode().value());
        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().token());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginResponse.getBody().token());

        return headers;
    }

    private Workspace createWorkspace(
            HttpHeaders headers,
            String name,
            String slug) {

        CreateWorkspaceRequest request =
                new CreateWorkspaceRequest(name, slug);

        ResponseEntity<Workspace> response =
                restTemplate.exchange(
                        url("/workspaces"),
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        Workspace.class
                );

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());

        return response.getBody();
    }

    private MonitorResponse createMonitor(
            HttpHeaders headers,
            Long workspaceId,
            String name) {

        MonitorRequest request = new MonitorRequest(
                workspaceId,
                name,
                "https://google.com",
                60,
                5000,
                MonitorType.HTTP
        );

        ResponseEntity<MonitorResponse> response =
                restTemplate.exchange(
                        url("/monitors"),
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        MonitorResponse.class
                );

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());

        return response.getBody();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
