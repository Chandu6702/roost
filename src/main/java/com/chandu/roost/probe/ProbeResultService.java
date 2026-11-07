package com.chandu.roost.probe;

import com.chandu.roost.audit.AuditLog;
import com.chandu.roost.monitor.Monitor;
import com.chandu.roost.monitor.MonitorNotFoundException;
import com.chandu.roost.audit.AuditLogRepository;
import com.chandu.roost.monitor.MonitorRepository;
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
