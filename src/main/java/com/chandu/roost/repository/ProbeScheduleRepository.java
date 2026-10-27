
package com.chandu.roost.repository;

import com.chandu.roost.entity.ProbeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ProbeScheduleRepository
        extends JpaRepository<ProbeSchedule, Long> {

    List<ProbeSchedule> findByNextProbeAtLessThanEqual(Instant cutoff);

    @Modifying
    @Query("""
        update ProbeSchedule s
        set s.nextProbeAt = :nextRunAt
        where s.id = :id
          and s.nextProbeAt <= :cutoff
        """)
    int claimDueSchedule(
            @Param("id") Long id,
            @Param("cutoff") Instant cutoff,
            @Param("nextRunAt") Instant nextRunAt
    );
}
