
package com.labresource.platform.service;

import com.labresource.platform.entity.Booking;
import com.labresource.platform.entity.BookingStatus;
import com.labresource.platform.entity.Equipment;
import com.labresource.platform.entity.EquipmentStatus;
import com.labresource.platform.entity.User;
import com.labresource.platform.repository.BookingRepository;
import com.labresource.platform.repository.EquipmentRepository;
import com.labresource.platform.repository.UserRepository;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    public BookingService(
            BookingRepository bookingRepository,
            EquipmentRepository equipmentRepository,
            UserRepository userRepository) {

        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
    }

    // Create a new booking
    @Transactional
    public Booking createBooking(
            Long userId,
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String purpose) {

        if (userId == null || equipmentId == null) {
            throw new IllegalArgumentException(
                    "User ID and equipment ID are required");
        }

        if (startTime == null || endTime == null
                || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "End time must be after start time");
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Booking start time must be in the future");
        }

        if (purpose == null || purpose.isBlank()
                || purpose.length() > 500) {
            throw new IllegalArgumentException(
                    "Purpose is required and must not exceed 500 characters");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Equipment not found"));

        if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
            throw new IllegalArgumentException(
                    "Equipment is not available for booking");
        }

        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.APPROVED
        );

        boolean overlapping =
                bookingRepository.existsOverlappingBooking(
                        equipmentId,
                        startTime,
                        endTime,
                        blockingStatuses
                );

        if (overlapping) {
            throw new IllegalArgumentException(
                    "Equipment already has a booking during this time");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setEquipment(equipment);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setPurpose(purpose);
        booking.setStatus(BookingStatus.PENDING);

        return bookingRepository.save(booking);
    }

    // View bookings belonging to one user
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUser(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        return bookingRepository.findByUser_Id(userId);
    }

    // View all bookings - access is checked in BookingController
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllWithDetails();
    }

    // View bookings for particular equipment
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByEquipment(Long equipmentId) {

        if (!equipmentRepository.existsById(equipmentId)) {
            throw new IllegalArgumentException("Equipment not found");
        }

        return bookingRepository.findByEquipment_Id(equipmentId);
    }

    // Cancel a user's own pending booking
    @Transactional
    public Booking cancelBooking(Long bookingId, Long userId) {

        Booking booking = bookingRepository
                .findByIdAndUserId(bookingId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found or you are not authorized to cancel it"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only pending bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        return bookingRepository.save(booking);
    }

    // Approve a pending booking
    @Transactional
    public Booking approveBooking(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only pending bookings can be approved");
        }

        booking.setStatus(BookingStatus.APPROVED);

        return bookingRepository.save(booking);
    }

    // Reject a pending booking
    @Transactional
    public Booking rejectBooking(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only pending bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);

        return bookingRepository.save(booking);
    }
}
