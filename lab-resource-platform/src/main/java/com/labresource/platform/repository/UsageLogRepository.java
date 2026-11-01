package com.labresource.platform.repository;

import com.labresource.platform.entity.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

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

    // Find completed usage sessions within a date range
    List<UsageLog> findByActualStartTimeBetweenAndActualEndTimeIsNotNull(
            LocalDateTime startTime,
            LocalDateTime endTime
    );
    // Find completed equipment usage logs, newest first
    List<UsageLog> findByActualEndTimeIsNotNullOrderByActualEndTimeDesc();

    // Find completed usage sessions for a specific equipment within a date range
    List<UsageLog> findByEquipmentIdAndActualStartTimeBetweenAndActualEndTimeIsNotNull(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );
}