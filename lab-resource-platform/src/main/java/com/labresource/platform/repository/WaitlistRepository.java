
package com.labresource.platform.repository;

import com.labresource.platform.entity.Waitlist;
import com.labresource.platform.entity.WaitlistStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WaitlistRepository
        extends JpaRepository<Waitlist, Long> {

    // Find waiting requests for equipment in FIFO order.
    List<Waitlist> findByEquipment_IdAndStatusOrderByCreatedAtAsc(
            Long equipmentId,
            WaitlistStatus status
    );

    // Find waiting users for the exact requested time slot.
    List<Waitlist>
    findByEquipment_IdAndStatusAndRequestedStartTimeAndRequestedEndTimeOrderByCreatedAtAsc(
            Long equipmentId,
            WaitlistStatus status,
            LocalDateTime requestedStartTime,
            LocalDateTime requestedEndTime
    );

    // Get all waitlist requests belonging to a user.
    List<Waitlist> findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    // Get all waitlist requests for particular equipment.
    List<Waitlist> findByEquipment_IdOrderByCreatedAtAsc(
            Long equipmentId
    );

    // Find offers whose expiry time has passed.
    List<Waitlist> findByStatusAndOfferExpiresAtLessThanEqual(
            WaitlistStatus status,
            LocalDateTime currentTime
    );

    // Check whether a user already has an active request.
    boolean existsByUser_IdAndEquipment_IdAndRequestedStartTimeAndRequestedEndTimeAndStatusIn(
            Long userId,
            Long equipmentId,
            LocalDateTime requestedStartTime,
            LocalDateTime requestedEndTime,
            List<WaitlistStatus> statuses
    );
    // Check whether a requested booking overlaps an active waitlist offer.

    // Check whether a requested booking overlaps an active offered slot.
    @Query("""
        SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END
        FROM Waitlist w
        WHERE w.equipment.id = :equipmentId
          AND w.status = :status
          AND w.offerExpiresAt > :now
          AND w.offeredStartTime IS NOT NULL
          AND w.offeredEndTime IS NOT NULL
          AND w.offeredStartTime < :requestedEndTime
          AND w.offeredEndTime > :requestedStartTime
        """)
    boolean existsActiveOfferOverlapping(
            @Param("equipmentId") Long equipmentId,
            @Param("status") WaitlistStatus status,
            @Param("now") LocalDateTime now,
            @Param("requestedStartTime") LocalDateTime requestedStartTime,
            @Param("requestedEndTime") LocalDateTime requestedEndTime
    );

}
