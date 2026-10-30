
package com.labresource.platform.repository;

import com.labresource.platform.entity.Booking;
import com.labresource.platform.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Fetch bookings belonging to a particular user
    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.equipment
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
        """)
    List<Booking> findByUser_Id(@Param("userId") Long userId);

    // Fetch all bookings with user and equipment details
    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.equipment
        ORDER BY b.createdAt DESC
        """)
    List<Booking> findAllWithDetails();

    // Fetch bookings for a particular equipment
    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.equipment
        WHERE b.equipment.id = :equipmentId
        ORDER BY b.createdAt DESC
        """)
    List<Booking> findByEquipment_Id(
            @Param("equipmentId") Long equipmentId
    );

    // Check whether the requested time overlaps another booking
    @Query("""
        SELECT COUNT(b) > 0
        FROM Booking b
        WHERE b.equipment.id = :equipmentId
          AND b.status IN :statuses
          AND b.startTime < :endTime
          AND b.endTime > :startTime
        """)
    boolean existsOverlappingBooking(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") List<BookingStatus> statuses
    );

    // Find a booking only when it belongs to the specified user
    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.equipment
        WHERE b.id = :bookingId
          AND b.user.id = :userId
        """)
    Optional<Booking> findByIdAndUserId(
            @Param("bookingId") Long bookingId,
            @Param("userId") Long userId
    );
}
