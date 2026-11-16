
package com.labresource.platform.service;

import com.labresource.platform.dto.BookingSuggestionDTO;
import com.labresource.platform.entity.*;

import com.labresource.platform.repository.BookingRepository;
import com.labresource.platform.repository.EquipmentRepository;
import com.labresource.platform.repository.EquipmentSharingRepository;
import com.labresource.platform.repository.UserRepository;
import com.labresource.platform.repository.WaitlistRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.labresource.platform.repository.EquipmentAccessGrantRepository;
import com.labresource.platform.repository.SharingAgreementRepository;
import com.labresource.platform.repository.EquipmentOperatingHoursRepository;
import java.math.BigDecimal;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final WaitlistRepository waitlistRepository;
    private final WaitlistService waitlistService;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentSharingRepository equipmentSharingRepository;
    private final UserRepository userRepository;
    private final UsageLogService usageLogService;
    private final LiveEquipmentWebSocketService liveEquipmentWebSocketService;
    private final ZoneId platformTimeZone;
    private final EquipmentAccessGrantRepository accessGrantRepository;
    private final SharingAgreementRepository sharingAgreementRepository;
    private final EquipmentOperatingHoursRepository operatingHoursRepository;

    public BookingService(
            BookingRepository bookingRepository,
            WaitlistRepository waitlistRepository,
            WaitlistService waitlistService,
            EquipmentRepository equipmentRepository,
            EquipmentSharingRepository equipmentSharingRepository,
            UserRepository userRepository,
            UsageLogService usageLogService,
            LiveEquipmentWebSocketService liveEquipmentWebSocketService,
            ZoneId platformTimeZone,
            EquipmentAccessGrantRepository accessGrantRepository,
            SharingAgreementRepository sharingAgreementRepository,
            EquipmentOperatingHoursRepository operatingHoursRepository) {

        this.bookingRepository = bookingRepository;
        this.waitlistRepository = waitlistRepository;
        this.waitlistService = waitlistService;
        this.equipmentRepository = equipmentRepository;
        this.equipmentSharingRepository = equipmentSharingRepository;
        this.userRepository = userRepository;
        this.usageLogService = usageLogService;
        this.liveEquipmentWebSocketService = liveEquipmentWebSocketService;
        this.platformTimeZone = platformTimeZone;
        this.accessGrantRepository = accessGrantRepository;
        this.sharingAgreementRepository = sharingAgreementRepository;
        this.operatingHoursRepository = operatingHoursRepository;
    }

    private LocalDateTime toUtc(LocalDateTime platformTime) {
        return platformTime.atZone(platformTimeZone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    // Create normal booking.
// Create normal booking.
// This method is also used by recurring bookings.
    @Transactional
    public Booking createBooking(
            Long userId,
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String purpose,
            UsageMode usageMode) {

        return createBooking(
                userId,
                equipmentId,
                startTime,
                endTime,
                purpose,
                usageMode,
                null
        );
    }
    // Create normal or external shared-equipment booking.
    @Transactional
    public Booking createBooking(
            Long userId,
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String purpose,
            UsageMode usageMode,
            Long sharingId) {

        if (userId == null || equipmentId == null) {
            throw new IllegalArgumentException(
                    "User ID and equipment ID are required");
        }

        if (startTime == null || endTime == null
                || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "End time must be after start time");
        }

        LocalDateTime platformNow = LocalDateTime.now(platformTimeZone);

        if (startTime.isBefore(platformNow)) {
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

        if (user.getInstitution() == null
                || equipment.getDepartment() == null
                || equipment.getDepartment().getInstitution() == null) {
            throw new IllegalArgumentException(
                    "User or equipment institution information is missing");
        }

        Long userInstitutionId = user.getInstitution().getId();

        Long ownerInstitutionId = equipment.getDepartment()
                .getInstitution().getId();

        EquipmentSharing sharing = null;

        // Institution-level sharing validation
        if (sharingId != null) {

            sharing = equipmentSharingRepository.findById(sharingId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Equipment sharing not found"));

            if (sharing.getStatus() != SharingStatus.ACTIVE) {
                throw new IllegalArgumentException(
                        "Equipment sharing is not active");
            }

            if (sharing.getEquipment() == null
                    || !sharing.getEquipment().getId().equals(equipmentId)) {
                throw new IllegalArgumentException(
                        "Sharing does not belong to this equipment");
            }

            if (sharing.getInstitution() == null
                    || !sharing.getInstitution().getId()
                    .equals(ownerInstitutionId)) {
                throw new IllegalArgumentException(
                        "Sharing does not belong to the equipment owner");
            }

            if (userInstitutionId.equals(ownerInstitutionId)) {
                throw new IllegalArgumentException(
                        "You cannot request external access to your own equipment");
            }

            // Verify active access grant
            EquipmentAccessGrant grant = accessGrantRepository
                    .findFirstByEquipmentIdAndPartnerInstitutionIdAndActiveTrue(
                            equipmentId,
                            userInstitutionId
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Your institution does not have an active access grant"));

            if (grant.getValidFrom() == null
                    || grant.getValidUntil() == null
                    || startTime.toLocalDate().isBefore(grant.getValidFrom())
                    || endTime.toLocalDate().isAfter(grant.getValidUntil())) {

                throw new IllegalArgumentException(
                        "Booking is outside the access grant period");
            }

            // Verify approved agreement
            SharingAgreement agreement = grant.getAgreement();

            if (agreement == null
                    || agreement.getStatus() != AgreementStatus.APPROVED) {
                throw new IllegalArgumentException(
                        "No approved sharing agreement exists");
            }

            if (agreement.getEquipmentSharing() == null
                    || !agreement.getEquipmentSharing().getId().equals(sharingId)) {
                throw new IllegalArgumentException(
                        "Access grant does not belong to this sharing agreement");
            }

            // Verify agreement date restrictions
            LocalDate bookingStartDate = startTime.toLocalDate();
            LocalDate bookingEndDate = endTime.toLocalDate();

            if (agreement.getStartDate() == null
                    || agreement.getEndDate() == null
                    || bookingStartDate.isBefore(agreement.getStartDate())
                    || bookingEndDate.isAfter(agreement.getEndDate())) {

                throw new IllegalArgumentException(
                        "Booking must fall within the agreement period");
            }

            // Verify maximum booking duration
            Integer maximumHours =
                    agreement.getMaximumBookingDurationHours();

            if (maximumHours == null || maximumHours <= 0) {
                throw new IllegalArgumentException(
                        "Maximum booking duration is not configured correctly");
            }

            long bookingDurationMinutes =
                    Duration.between(startTime, endTime).toMinutes();

            long maximumDurationMinutes = maximumHours * 60L;

            if (bookingDurationMinutes > maximumDurationMinutes) {
                throw new IllegalArgumentException(
                        "Booking exceeds the maximum allowed duration of "
                                + maximumHours + " hours");
            }

            // Verify minimum advance notice
            Integer minimumNotice =
                    agreement.getMinimumAdvanceNoticeHours();

            if (minimumNotice == null || minimumNotice < 0) {
                throw new IllegalArgumentException(
                        "Minimum advance notice is not configured correctly");
            }

            LocalDateTime earliestAllowedBooking =
                    platformNow.plusHours(minimumNotice);

            if (startTime.isBefore(earliestAllowedBooking)) {
                throw new IllegalArgumentException(
                        "Booking does not satisfy the minimum advance notice of "
                                + minimumNotice + " hours");
            }

            // Verify weekly sharing limit
            BigDecimal weeklyLimit =
                    agreement.getWeeklySharingLimitHours();

            if (weeklyLimit == null
                    || weeklyLimit.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Weekly sharing limit is not configured correctly");
            }

            // Calculate Monday 00:00 to next Monday 00:00
            // using platform local time
            LocalDate weekStartDate = startTime.toLocalDate()
                    .with(java.time.temporal.TemporalAdjusters
                            .previousOrSame(DayOfWeek.MONDAY));

            LocalDateTime weekStart = weekStartDate.atStartOfDay();
            LocalDateTime weekEnd =
                    weekStartDate.plusWeeks(1).atStartOfDay();

            // Convert week boundaries to UTC because
            // booking records are stored in UTC
            LocalDateTime utcWeekStart = toUtc(weekStart);
            LocalDateTime utcWeekEnd = toUtc(weekEnd);

            // Find existing bookings for the same equipment,
            // sharing listing and partner institution
            List<Booking> weeklyBookings =
                    bookingRepository.findPartnerSharingBookingsInRange(
                            equipmentId,
                            sharingId,
                            userInstitutionId,
                            utcWeekStart,
                            utcWeekEnd,
                            List.of(
                                    BookingStatus.PENDING,
                                    BookingStatus.APPROVED,
                                    BookingStatus.IN_USE
                            )
                    );

            // Count only the minutes that fall inside this week
            long existingBookedMinutes = weeklyBookings.stream()
                    .mapToLong(booking -> {

                        LocalDateTime overlapStart =
                                booking.getStartTime().isAfter(utcWeekStart)
                                        ? booking.getStartTime()
                                        : utcWeekStart;

                        LocalDateTime overlapEnd =
                                booking.getEndTime().isBefore(utcWeekEnd)
                                        ? booking.getEndTime()
                                        : utcWeekEnd;

                        if (!overlapEnd.isAfter(overlapStart)) {
                            return 0L;
                        }

                        return Duration.between(
                                overlapStart,
                                overlapEnd
                        ).toMinutes();
                    })
                    .sum();

            // Add the new booking request
            long requestedMinutes = Duration.between(
                    toUtc(startTime),
                    toUtc(endTime)
            ).toMinutes();

            long totalBookedMinutes =
                    existingBookedMinutes + requestedMinutes;

            BigDecimal maximumAllowedMinutes =
                    weeklyLimit.multiply(BigDecimal.valueOf(60));

            if (BigDecimal.valueOf(totalBookedMinutes)
                    .compareTo(maximumAllowedMinutes) > 0) {

                throw new IllegalArgumentException(
                        "Weekly sharing limit exceeded. Maximum allowed: "
                                + weeklyLimit + " hours per week.");
            }

        } else {

            if (!userInstitutionId.equals(ownerInstitutionId)) {
                throw new IllegalArgumentException(
                        "External equipment requires a sharing agreement");
            }
        }

        // Validate equipment operating hours
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            throw new IllegalArgumentException(
                    "Booking cannot cross into another day");
        }

        List<EquipmentOperatingHours> operatingHours =
                operatingHoursRepository
                        .findByEquipment_IdOrderByDayOfWeekAsc(equipmentId);

        int bookingDay = startTime.getDayOfWeek().getValue();

        LocalTime bookingStart = startTime.toLocalTime();
        LocalTime bookingEnd = endTime.toLocalTime();

        if (operatingHours.isEmpty()) {

            // Default schedule: Monday to Friday, 9 AM to 5 PM
            if (bookingDay < 1 || bookingDay > 5
                    || bookingStart.isBefore(LocalTime.of(9, 0))
                    || bookingEnd.isAfter(LocalTime.of(17, 0))) {

                throw new IllegalArgumentException(
                        "Booking must be within default operating hours: Monday to Friday, 9 AM to 5 PM");
            }

        } else {

            // Use configured operating hours
            EquipmentOperatingHours dailyHours = operatingHours.stream()
                    .filter(hours -> hours.getDayOfWeek() == bookingDay)
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Equipment is closed on the selected day"));

            if (dailyHours.getOpenTime() == null
                    || dailyHours.getCloseTime() == null
                    || bookingStart.isBefore(dailyHours.getOpenTime())
                    || bookingEnd.isAfter(dailyHours.getCloseTime())) {

                throw new IllegalArgumentException(
                        "Booking must be within equipment operating hours");
            }
        }

        // Convert platform time to UTC
        LocalDateTime utcStartTime = toUtc(startTime);
        LocalDateTime utcEndTime = toUtc(endTime);

        // Check existing booking conflicts
        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.APPROVED,
                BookingStatus.IN_USE
        );

        boolean overlapping =
                bookingRepository.existsOverlappingBooking(
                        equipmentId,
                        utcStartTime,
                        utcEndTime,
                        blockingStatuses
                );

        if (overlapping) {
            throw new IllegalArgumentException(
                    "Equipment already has a booking during this time");
        }

        // Check waitlist slot holds
        boolean slotHeldForWaitlist =
                waitlistRepository.existsActiveOfferOverlapping(
                        equipmentId,
                        WaitlistStatus.NOTIFIED,
                        LocalDateTime.now(ZoneOffset.UTC),
                        utcStartTime,
                        utcEndTime
                );

        if (slotHeldForWaitlist) {
            throw new IllegalArgumentException(
                    "This time slot is temporarily held for a waitlist user. Please try another time.");
        }

        // Create booking
        Booking booking = new Booking();

        booking.setUser(user);
        booking.setEquipment(equipment);
        booking.setStartTime(utcStartTime);
        booking.setEndTime(utcEndTime);
        booking.setPurpose(purpose.trim());
        booking.setUsageMode(usageMode);
        booking.setStatus(BookingStatus.PENDING);
        booking.setSharing(sharing);

        return bookingRepository.save(booking);
    }





    // Create recurring bookings.
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

        if (startTime == null || endTime == null
                || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "Invalid recurring booking time");
        }

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

        String recurringSeriesId = UUID.randomUUID().toString();

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

            booking.setRecurringSeriesId(recurringSeriesId);
            bookingRepository.save(booking);
            bookings.add(booking);
        }

        return bookings;
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUser(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        return bookingRepository.findByUser_Id(userId);
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsByEquipment(Long equipmentId) {

        if (!equipmentRepository.existsById(equipmentId)) {
            throw new IllegalArgumentException("Equipment not found");
        }

        return bookingRepository.findByEquipment_Id(equipmentId);
    }

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

        Booking savedBooking = bookingRepository.save(booking);

        waitlistService.offerNextWaitlistUser(
                booking.getEquipment().getId(),
                booking.getStartTime(),
                booking.getEndTime()
        );

        return savedBooking;
    }

    // Verify that manager owns the equipment's institution.
    private void validateBookingManager(Booking booking, Long managerId) {

        User manager = userRepository.findById(managerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Manager not found"));

        if (manager.getRole() == null
                || !"LAB_MANAGER".equals(manager.getRole().toString())) {
            throw new IllegalArgumentException(
                    "Only Lab Managers can manage bookings");
        }

        if (manager.getInstitution() == null
                || booking.getEquipment().getDepartment() == null
                || booking.getEquipment().getDepartment().getInstitution() == null) {
            throw new IllegalArgumentException(
                    "Institution information is missing");
        }

        Long managerInstitutionId = manager.getInstitution().getId();

        Long equipmentInstitutionId = booking.getEquipment()
                .getDepartment()
                .getInstitution()
                .getId();

        if (!managerInstitutionId.equals(equipmentInstitutionId)) {
            throw new IllegalArgumentException(
                    "You can only manage bookings for equipment owned by your institution");
        }
    }

    // Approve booking.
    @Transactional
    public Booking approveBooking(Long bookingId, Long managerId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only pending bookings can be approved");
        }

        validateBookingManager(booking, managerId);

        booking.setStatus(BookingStatus.APPROVED);

        Booking savedBooking = bookingRepository.save(booking);

        savedBooking.getUser().getId();
        savedBooking.getUser().getFullName();
        savedBooking.getUser().getRole();

        savedBooking.getEquipment().getId();
        savedBooking.getEquipment().getName();
        savedBooking.getEquipment().getAssetTag();

        return savedBooking;
    }

    // Approve recurring series.
    @Transactional
    public List<Booking> approveRecurringSeries(
            String seriesId, Long managerId) {

        List<Booking> bookings =
                bookingRepository.findByRecurringSeriesIdOrderByStartTimeAsc(seriesId);

        if (bookings.isEmpty()) {
            throw new IllegalArgumentException(
                    "Recurring booking series not found");
        }

        boolean allPending = bookings.stream()
                .allMatch(booking ->
                        booking.getStatus() == BookingStatus.PENDING);

        if (!allPending) {
            throw new IllegalArgumentException(
                    "All bookings in the series must be pending");
        }

        for (Booking booking : bookings) {
            validateBookingManager(booking, managerId);
        }

        for (Booking booking : bookings) {
            booking.setStatus(BookingStatus.APPROVED);

            booking.getUser().getId();
            booking.getUser().getFullName();
            booking.getUser().getRole();

            booking.getEquipment().getId();
            booking.getEquipment().getName();
            booking.getEquipment().getAssetTag();
        }

        return bookingRepository.saveAll(bookings);
    }

    // Reject booking.
    @Transactional
    public Booking rejectBooking(Long bookingId, Long managerId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only pending bookings can be rejected");
        }

        validateBookingManager(booking, managerId);

        booking.setStatus(BookingStatus.REJECTED);

        Booking savedBooking = bookingRepository.save(booking);

        waitlistService.offerNextWaitlistUser(
                booking.getEquipment().getId(),
                booking.getStartTime(),
                booking.getEndTime()
        );

        return savedBooking;
    }

    // Reject recurring series.
    @Transactional
    public List<Booking> rejectRecurringSeries(
            String seriesId, Long managerId) {

        List<Booking> bookings =
                bookingRepository.findByRecurringSeriesIdOrderByStartTimeAsc(seriesId);

        if (bookings.isEmpty()) {
            throw new IllegalArgumentException(
                    "Recurring booking series not found");
        }

        boolean allPending = bookings.stream()
                .allMatch(booking ->
                        booking.getStatus() == BookingStatus.PENDING);

        if (!allPending) {
            throw new IllegalArgumentException(
                    "All bookings in the series must be pending");
        }

        for (Booking booking : bookings) {
            validateBookingManager(booking, managerId);
        }

        for (Booking booking : bookings) {
            booking.setStatus(BookingStatus.REJECTED);
        }

        List<Booking> savedBookings =
                bookingRepository.saveAll(bookings);

        for (Booking booking : savedBookings) {

            waitlistService.offerNextWaitlistUser(
                    booking.getEquipment().getId(),
                    booking.getStartTime(),
                    booking.getEndTime()
            );

            booking.getUser().getId();
            booking.getUser().getFullName();
            booking.getUser().getRole();

            booking.getEquipment().getId();
            booking.getEquipment().getName();
            booking.getEquipment().getAssetTag();
        }

        return savedBookings;
    }

    // Check in.
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

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        LocalDateTime allowedStart =
                booking.getStartTime().minusMinutes(15);

        LocalDateTime checkInDeadline =
                booking.getStartTime().plusMinutes(15);

        if (now.isBefore(allowedStart)
                || now.isAfter(checkInDeadline)) {
            throw new IllegalArgumentException(
                    "Check-in is allowed from 15 minutes before booking start until 15 minutes after booking start");
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

    // Check out.
    @Transactional
    public Booking checkOut(Long bookingId, Long userId) {

        Booking booking = bookingRepository
                .findByIdAndUserId(bookingId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found or unauthorized"));

        if (booking.getStatus() != BookingStatus.IN_USE) {
            throw new IllegalArgumentException(
                    "Only active bookings can be checked out");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        UsageLog usageLog =
                usageLogService.getOpenUsageLog(bookingId);

        if (usageLog == null) {
            throw new IllegalStateException(
                    "No open usage log found for this booking");
        }

        if (now.isBefore(usageLog.getActualStartTime())) {
            throw new IllegalStateException(
                    "Checkout time cannot be before check-in time");
        }

        usageLog.setActualEndTime(now);

        long duration = Duration.between(
                usageLog.getActualStartTime(),
                now
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

        if (booking.getEndTime().isAfter(now)) {
            waitlistService.offerNextWaitlistUser(
                    equipment.getId(),
                    now,
                    booking.getEndTime()
            );
        }

        liveEquipmentWebSocketService.broadcastEquipmentStatus();

        return savedBooking;
    }

    // Suggest alternative booking slots.
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

        if (requestedStartTime.isBefore(
                LocalDateTime.now(platformTimeZone))) {
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
                requestedStartTime,
                requestedEndTime
        ).toMinutes();

        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.APPROVED,
                BookingStatus.IN_USE
        );

        List<BookingSuggestionDTO> suggestions = new ArrayList<>();

        LocalDateTime candidateStart =
                requestedStartTime.plusMinutes(30);

        LocalDateTime searchLimit =
                requestedStartTime.plusDays(7);

        while (suggestions.size() < suggestionCount
                && candidateStart.plusMinutes(durationMinutes)
                .isBefore(searchLimit)) {

            LocalDateTime candidateEnd =
                    candidateStart.plusMinutes(durationMinutes);

            boolean overlapping =
                    bookingRepository.existsOverlappingBooking(
                            equipmentId,
                            toUtc(candidateStart),
                            toUtc(candidateEnd),
                            blockingStatuses
                    );

            if (!overlapping) {
                suggestions.add(new BookingSuggestionDTO(
                        candidateStart,
                        candidateEnd,
                        "Alternative slot available"
                ));
            }

            candidateStart = candidateStart.plusMinutes(30);
        }

        return suggestions;
    }
}
