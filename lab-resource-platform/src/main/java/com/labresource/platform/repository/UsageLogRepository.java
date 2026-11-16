
package com.labresource.platform.repository;

import com.labresource.platform.entity.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {

    Optional<UsageLog> findTopByBookingIdAndActualEndTimeIsNullOrderByActualStartTimeDesc(
            Long bookingId
    );

    List<UsageLog> findByActualStartTimeBetween(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    List<UsageLog> findByActualStartTimeBetweenAndActualEndTimeIsNotNull(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    List<UsageLog> findByActualEndTimeIsNotNullOrderByActualEndTimeDesc();

    List<UsageLog> findByEquipmentIdAndActualStartTimeBetweenAndActualEndTimeIsNotNull(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    List<UsageLog> findByBookingId(Long bookingId);

    @Query("""
            SELECT u FROM UsageLog u
            WHERE u.actualStartTime < :endTime
            AND (u.actualEndTime IS NULL OR u.actualEndTime > :startTime)
            """)
    List<UsageLog> findOverlappingUsageLogs(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
