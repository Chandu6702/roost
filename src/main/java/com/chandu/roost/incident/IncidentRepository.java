package com.chandu.roost.incident;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    boolean existsByMonitorIdAndStatusIn(
            Long monitorId, Collection<IncidentStatus> statuses);

    List<Incident> findByMonitorIdAndStatusIn(
            Long monitorId, Collection<IncidentStatus> statuses);

    List<Incident> findByMonitorIdInOrderByOpenedAtDesc(
            Collection<Long> monitorIds, Pageable pageable);

    long countByStatus(IncidentStatus status);
}
