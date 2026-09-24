package com.chandu.roost.repository;

import com.chandu.roost.entity.ProbeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProbeScheduleRepository
        extends JpaRepository<ProbeSchedule, Long> {
}