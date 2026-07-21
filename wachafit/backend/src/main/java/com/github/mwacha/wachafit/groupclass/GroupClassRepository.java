package com.github.mwacha.wachafit.groupclass;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GroupClassRepository extends JpaRepository<GroupClass, UUID> {
    List<GroupClass> findByActive(boolean active);

    @Query("""
        SELECT gc FROM GroupClass gc
        WHERE gc.id <> :id
          AND gc.active = true
          AND gc.scheduleType = 'FIXED'
          AND gc.startTime = :startTime
          AND gc.endTime = :endTime
    """)
    List<GroupClass> findActiveFixedConflicts(@Param("id") UUID id,
                                              @Param("startTime") String startTime,
                                              @Param("endTime") String endTime);
}
