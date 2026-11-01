package com.labresource.platform.repository;

import com.labresource.platform.entity.Waitlist;
import com.labresource.platform.entity.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    // Get active waiting requests for equipment in FIFO order
    List<Waitlist> findByEquipment_IdAndStatusOrderByCreatedAtAsc(
            Long equipmentId,
            WaitlistStatus status
    );

    // Get all waitlist requests belonging to a user
    List<Waitlist> findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    // Get all waitlist requests for particular equipment
    List<Waitlist> findByEquipment_IdOrderByCreatedAtAsc(
            Long equipmentId
    );

    // Check whether a user already has an active request for equipment
    boolean existsByUser_IdAndEquipment_IdAndStatusIn(
            Long userId,
            Long equipmentId,
            List<WaitlistStatus> statuses
    );
}