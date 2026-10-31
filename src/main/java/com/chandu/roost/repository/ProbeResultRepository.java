package com.chandu.roost.repository;

import com.chandu.roost.entity.ProbeResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProbeResultRepository extends JpaRepository<ProbeResult, Long> {

    Page<ProbeResult> findByMonitorId(Long monitorId, Pageable pageable);

    long countByMonitorId(Long monitorId);

    /** Latest probe outcome for a monitor — the monitor's "current status". */
    Optional<ProbeResult> findFirstByMonitorIdOrderByProbedAtDesc(Long monitorId);
}
