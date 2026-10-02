package com.chandu.roost.repository;

import com.chandu.roost.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByIncidentId(Long incidentId);

    long countByMonitorIdAndAction(Long monitorId, String action);
}
