package com.chandu.roost.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.chandu.roost.dto.CreateWorkspaceRequest;
import com.chandu.roost.entity.Workspace;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.chandu.roost.dto.MonitorRequest;
import com.chandu.roost.dto.MonitorResponse;
import com.chandu.roost.entity.MonitorType;

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

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void createWorkspace_returns201() {

        CreateWorkspaceRequest request =
                new CreateWorkspaceRequest(
                        "Integration Test Workspace",
                        "integration-test"
                );

        ResponseEntity<Workspace> response =
                restTemplate.postForEntity(
                        "http://localhost:" + port + "/workspaces",
                        request,
                        Workspace.class
                );

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
                "Integration Test Workspace",
                response.getBody().getName()
        );
        Long workspaceId = response.getBody().getId();

        assertNotNull(workspaceId);

        MonitorRequest monitorRequest = new MonitorRequest(
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
        assertEquals("Google Monitor", monitorResponse.getBody().name());

        Long monitorId = monitorResponse.getBody().id();

        ResponseEntity<String> getResponse =
                restTemplate.getForEntity(
                        "http://localhost:" + port + "/monitors/" + monitorId,
                        String.class
                );

        assertEquals(200, getResponse.getStatusCode().value());
        assertNotNull(getResponse.getBody());

        System.out.println(getResponse.getBody());
    }
}

