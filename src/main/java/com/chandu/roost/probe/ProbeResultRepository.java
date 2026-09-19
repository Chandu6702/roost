package com.chandu.roost.probe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProbeResultRepository extends JpaRepository<ProbeResult, Long> {

    Page<ProbeResult> findByMonitorId(Long monitorId, Pageable pageable);
}