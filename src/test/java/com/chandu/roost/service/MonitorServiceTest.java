package com.chandu.roost.service;

import com.chandu.roost.entity.ProbeResult;
import com.chandu.roost.repository.MonitorRepository;
import com.chandu.roost.repository.ProbeResultRepository;
import com.chandu.roost.repository.ProbeScheduleRepository;
import com.chandu.roost.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class MonitorServiceTest {

    private MonitorService monitorService;

    @BeforeEach
    void setUp() {
        monitorService = new MonitorService(
                mock(MonitorRepository.class),
                mock(WorkspaceRepository.class),
                mock(ProbeResultRepository.class),
                mock(ProbeScheduleRepository.class),
                mock(AuditService.class)
        );
    }

    @Test
    void calculateUptimePercent_allUp_returns100Percent() {
        List<ProbeResult> results = List.of(
                new ProbeResult(null, Instant.now(), "UP", 100, null),
                new ProbeResult(null, Instant.now(), "UP", 120, null),
                new ProbeResult(null, Instant.now(), "UP", 90, null),
                new ProbeResult(null, Instant.now(), "UP", 110, null),
                new ProbeResult(null, Instant.now(), "UP", 105, null),
                new ProbeResult(null, Instant.now(), "UP", 95, null),
                new ProbeResult(null, Instant.now(), "UP", 130, null),
                new ProbeResult(null, Instant.now(), "UP", 115, null),
                new ProbeResult(null, Instant.now(), "UP", 80, null),
                new ProbeResult(null, Instant.now(), "UP", 125, null)
        );

        double result = monitorService.calculateUptimePercent(results);

        assertEquals(100.0, result);
    }

    @Test
    void calculateUptimePercent_allDown_returns0Percent() {
        List<ProbeResult> results = List.of(
                new ProbeResult(null, Instant.now(), "DOWN", 100, "Connection failed"),
                new ProbeResult(null, Instant.now(), "DOWN", 120, "Connection failed"),
                new ProbeResult(null, Instant.now(), "DOWN", 90, "Connection failed"),
                new ProbeResult(null, Instant.now(), "DOWN", 110, "Connection failed"),
                new ProbeResult(null, Instant.now(), "DOWN", 105, "Connection failed"),
                new ProbeResult(null, Instant.now(), "DOWN", 95, "Connection failed"),
                new ProbeResult(null, Instant.now(), "DOWN", 130, "Connection failed"),
                new ProbeResult(null, Instant.now(), "DOWN", 115, "Connection failed"),
                new ProbeResult(null, Instant.now(), "DOWN", 80, "Connection failed"),
                new ProbeResult(null, Instant.now(), "DOWN", 125, "Connection failed")
        );

        double result = monitorService.calculateUptimePercent(results);

        assertEquals(0.0, result);
    }

    @Test
    void calculateUptimePercent_mixedResults_returnsCorrectPercentage() {
        List<ProbeResult> results = List.of(
                new ProbeResult(null, Instant.now(), "UP", 100, null),
                new ProbeResult(null, Instant.now(), "UP", 120, null),
                new ProbeResult(null, Instant.now(), "UP", 90, null),
                new ProbeResult(null, Instant.now(), "UP", 110, null),
                new ProbeResult(null, Instant.now(), "UP", 105, null),
                new ProbeResult(null, Instant.now(), "UP", 95, null),
                new ProbeResult(null, Instant.now(), "UP", 130, null),
                new ProbeResult(null, Instant.now(), "DOWN", 115, "Connection failed"),
                new ProbeResult(null, Instant.now(), "DOWN", 80, "Connection failed"),
                new ProbeResult(null, Instant.now(), "DOWN", 125, "Connection failed")
        );

        double result = monitorService.calculateUptimePercent(results);

        assertEquals(70.0, result);
    }

    @Test
    void calculateUptimePercent_emptyList_returns0Percent() {
        double result = monitorService.calculateUptimePercent(List.of());

        assertEquals(0.0, result);
    }

    @Test
    void calculateUptimePercent_unsupportedStatus_doesNotCountAsUp() {
        List<ProbeResult> results = List.of(
                new ProbeResult(null, Instant.now(), "UP", 100, null),
                new ProbeResult(null, Instant.now(), "UNKNOWN", 100, "Unknown result"),
                new ProbeResult(null, Instant.now(), "DOWN", 100, "Connection failed"),
                new ProbeResult(null, Instant.now(), "UP", 100, null)
        );

        double result = monitorService.calculateUptimePercent(results);

        assertEquals(50.0, result);
    }

    @Test
    void calculateUptimePercent_nullList_throwsNullPointerException() {
        assertThrows(
                NullPointerException.class,
                () -> monitorService.calculateUptimePercent(null)
        );
    }

    @ParameterizedTest
    @CsvSource({
            "10, 0, 100.0",
            "0, 10, 0.0",
            "7, 3, 70.0",
            "1, 9, 10.0"
    })
    void calculateUptimePercent_parameterized_returnsExpectedPercentage(
            int upCount,
            int downCount,
            double expected) {

        List<ProbeResult> results = new java.util.ArrayList<>();

        for (int i = 0; i < upCount; i++) {
            results.add(
                    new ProbeResult(
                            null,
                            Instant.now(),
                            "UP",
                            100,
                            null
                    )
            );
        }

        for (int i = 0; i < downCount; i++) {
            results.add(
                    new ProbeResult(
                            null,
                            Instant.now(),
                            "DOWN",
                            100,
                            "Connection failed"
                    )
            );
        }

        double result = monitorService.calculateUptimePercent(results);

        assertEquals(expected, result);
    }
}