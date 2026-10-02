package com.chandu.roost.service;

import com.chandu.roost.dto.ProbeResultRequest;
import com.chandu.roost.dto.ProbeResultResponse;
import com.chandu.roost.entity.AuditLog;
import com.chandu.roost.entity.Monitor;
import com.chandu.roost.entity.ProbeResult;
import com.chandu.roost.error.MonitorNotFoundException;
import com.chandu.roost.repository.AuditLogRepository;
import com.chandu.roost.repository.MonitorRepository;
import com.chandu.roost.repository.ProbeResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ProbeResultService {

    private final MonitorRepository monitorRepository;
    private final ProbeResultRepository probeResultRepository;
    private final AuditLogRepository auditLogRepository;

    public ProbeResultService(
            MonitorRepository monitorRepository,
            ProbeResultRepository probeResultRepository,
            AuditLogRepository auditLogRepository) {

        this.monitorRepository = monitorRepository;
        this.probeResultRepository = probeResultRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public ProbeResultResponse create(
            Long monitorId,
            ProbeResultRequest request) {

        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() ->
                        new MonitorNotFoundException(monitorId));

        ProbeResult probeResult = new ProbeResult(
                monitor,
                request.probedAt(),
                request.status(),
                request.latencyMs(),
                request.errorMessage()
        );

        ProbeResult saved = probeResultRepository.save(probeResult);

        AuditLog auditLog = new AuditLog(
                monitor,
                "PROBE_RESULT_RECORDED",
                Instant.now()
        );

        auditLogRepository.save(auditLog);

        return new ProbeResultResponse(
                saved.getId(),
                saved.getProbedAt(),
                saved.getStatus(),
                saved.getLatencyMs(),
                saved.getErrorMessage()
        );
    }
}
