package com.chandu.roost.service;

import com.chandu.roost.entity.AuditLog;
import com.chandu.roost.entity.Incident;
import com.chandu.roost.entity.Monitor;
import com.chandu.roost.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logStateChange(Incident incident, String action) {
        AuditLog auditLog = new AuditLog(
                incident,
                action,
                Instant.now()
        );

        auditLogRepository.save(auditLog);
    }

    @Transactional
    public void logMonitorEvent(Monitor monitor, String action) {
        AuditLog auditLog = new AuditLog(
                monitor,
                action,
                Instant.now()
        );

        auditLogRepository.save(auditLog);
    }
}