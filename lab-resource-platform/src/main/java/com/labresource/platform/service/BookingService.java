
package com.labresource.platform.service;

import com.labresource.platform.dto.BookingSuggestionDTO;
import com.labresource.platform.entity.Booking;
import com.labresource.platform.entity.BookingStatus;
import com.labresource.platform.entity.Equipment;
import com.labresource.platform.entity.EquipmentStatus;
import com.labresource.platform.entity.User;
import com.labresource.platform.entity.UsageLog;
import com.labresource.platform.entity.UsageMode;

import com.labresource.platform.repository.BookingRepository;
import com.labresource.platform.repository.EquipmentRepository;
import com.labresource.platform.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final UsageLogService usageLogService;
    private final LiveEquipmentWebSocketService liveEquipmentWebSocketService;

    public BookingService(
            BookingRepository bookingRepository,
            EquipmentRepository equipmentRepository,
            UserRepository userRepository,
            UsageLogService usageLogService,
            LiveEquipmentWebSocketService liveEquipmentWebSocketService) {

        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.usageLogService = usageLogService;
        this.liveEquipmentWebSocketService = liveEquipmentWebSocketService;
    }

    // Create a new normal booking
    @Transactional
    public Booking createBooking(
            Long userId,
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String purpose,
            UsageMode usageMode) {

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

        if (usageMode == null) {
            throw new IllegalArgumentException(
                    "Usage mode is required");
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
        booking.setUsageMode(usageMode);
        booking.setStatus(BookingStatus.PENDING);

        return bookingRepository.save(booking);
    }

    // Create recurring bookings
    @Transactional
    public List<Booking> createRecurringBookings(
            Long userId,
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String purpose,
            UsageMode usageMode,
            String recurrenceType,
            Integer recurrenceCount) {

        if (recurrenceType == null || recurrenceType.isBlank()) {
            throw new IllegalArgumentException(
                    "Recurrence type is required");
        }

        if (recurrenceCount == null
                || recurrenceCount < 2
                || recurrenceCount > 30) {
            throw new IllegalArgumentException(
                    "Recurrence count must be between 2 and 30");
        }

        String type = recurrenceType.trim().toUpperCase(Locale.ROOT);

        if (!List.of("DAILY", "WEEKLY", "MONTHLY").contains(type)) {
            throw new IllegalArgumentException(
                    "Recurrence type must be DAILY, WEEKLY or MONTHLY");
        }

        List<Booking> bookings = new ArrayList<>();

        for (int i = 0; i < recurrenceCount; i++) {

            LocalDateTime occurrenceStart;
            LocalDateTime occurrenceEnd;

            switch (type) {
                case "DAILY" -> {
                    occurrenceStart = startTime.plusDays(i);
                    occurrenceEnd = endTime.plusDays(i);
                }

                case "WEEKLY" -> {
                    occurrenceStart = startTime.plusWeeks(i);
                    occurrenceEnd = endTime.plusWeeks(i);
                }

                case "MONTHLY" -> {
                    occurrenceStart = startTime.plusMonths(i);
                    occurrenceEnd = endTime.plusMonths(i);
                }

                default -> throw new IllegalArgumentException(
                        "Unsupported recurrence type");
            }

            Booking booking = createBooking(
                    userId,
                    equipmentId,
                    occurrenceStart,
                    occurrenceEnd,
                    purpose,
                    usageMode
            );

            bookings.add(booking);
        }

        return bookings;
    }

    // View bookings belonging to one user
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUser(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        return bookingRepository.findByUser_Id(userId);
    }

    // View all bookings
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

    // Check in
    @Transactional
    public Booking checkIn(Long bookingId, Long userId) {

        Booking booking = bookingRepository
                .findByIdAndUserId(bookingId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found or unauthorized"));

        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Only approved bookings can be checked in");
        }

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime allowedStart =
                booking.getStartTime().minusMinutes(15);

        if (now.isBefore(allowedStart)
                || now.isAfter(booking.getEndTime())) {
            throw new IllegalArgumentException(
                    "Check-in is allowed only from 15 minutes before start until booking end");
        }

        Equipment equipment = booking.getEquipment();

        if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
            throw new IllegalArgumentException(
                    "Equipment is not available for check-in");
        }

        booking.setStatus(BookingStatus.IN_USE);

        UsageLog usageLog = new UsageLog();
        usageLog.setBooking(booking);
        usageLog.setUser(booking.getUser());
        usageLog.setEquipment(equipment);
        usageLog.setActualStartTime(now);
        usageLog.setUsageType("BOOKING");
        usageLog.setUsageMode(booking.getUsageMode());

        usageLogService.saveUsageLog(usageLog);

        equipment.setStatus(EquipmentStatus.BOOKED);
        equipmentRepository.save(equipment);

        Booking savedBooking = bookingRepository.save(booking);

        liveEquipmentWebSocketService.broadcastEquipmentStatus();

        return savedBooking;
    }

    // Check out
    @Transactional
    public Booking checkOut(Long bookingId, Long userId) {

        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found or unauthorized"));

        if (booking.getStatus() != BookingStatus.IN_USE) {
            throw new IllegalArgumentException(
                    "Only active bookings can be checked out");
        }

        LocalDateTime now = LocalDateTime.now();

        UsageLog usageLog =
                usageLogService.getOpenUsageLog(bookingId);

        usageLog.setActualEndTime(now);

        long duration = Duration.between(
                usageLog.getActualStartTime(), now
        ).toMinutes();

        usageLog.setActualDuration(duration);

        usageLogService.saveUsageLog(usageLog);

        booking.setStatus(BookingStatus.COMPLETED);

        Equipment equipment = booking.getEquipment();

        if (equipment.getStatus() == EquipmentStatus.BOOKED) {
            equipment.setStatus(EquipmentStatus.AVAILABLE);
            equipmentRepository.save(equipment);
        }

        Booking savedBooking = bookingRepository.save(booking);

        liveEquipmentWebSocketService.broadcastEquipmentStatus();

        return savedBooking;
    }

    // Suggest alternative booking slots
    @Transactional(readOnly = true)
    public List<BookingSuggestionDTO> suggestAlternativeSlots(
            Long equipmentId,
            LocalDateTime requestedStartTime,
            LocalDateTime requestedEndTime,
            int suggestionCount) {

        if (requestedStartTime == null || requestedEndTime == null
                || !requestedEndTime.isAfter(requestedStartTime)) {
            throw new IllegalArgumentException("Invalid booking time");
        }

        if (requestedStartTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Requested time must be in the future");
        }

        if (suggestionCount < 1 || suggestionCount > 10) {
            throw new IllegalArgumentException(
                    "Suggestion count must be between 1 and 10");
        }

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Equipment not found"));

        if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
            throw new IllegalArgumentException(
                    "Equipment is currently unavailable");
        }

        long durationMinutes = Duration.between(
                requestedStartTime, requestedEndTime).toMinutes();

        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.APPROVED,
                BookingStatus.IN_USE
        );

        List<BookingSuggestionDTO> suggestions = new ArrayList<>();

        LocalDateTime candidateStart = requestedStartTime.plusMinutes(30);

        LocalDateTime searchLimit = requestedStartTime.plusDays(7);

        while (suggestions.size() < suggestionCount
                && candidateStart.plusMinutes(durationMinutes)
                .isBefore(searchLimit)) {

            LocalDateTime candidateEnd =
                    candidateStart.plusMinutes(durationMinutes);

            boolean overlapping =
                    bookingRepository.existsOverlappingBooking(
                            equipmentId,
                            candidateStart,
                            candidateEnd,
                            blockingStatuses
                    );

            if (!overlapping) {
                suggestions.add(new BookingSuggestionDTO(
                        candidateStart,
                        candidateEnd,
                        "Alternative slot available"
                ));

                candidateStart = candidateStart.plusMinutes(30);
            } else {
                candidateStart = candidateStart.plusMinutes(30);
            }
        }

        return suggestions;
    }
}
