
package com.labresource.platform.service;

import com.labresource.platform.dto.WaitlistRequestDTO;
import com.labresource.platform.dto.WaitlistResponseDTO;
import com.labresource.platform.entity.*;
import com.labresource.platform.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class WaitlistService {

    private static final int OFFER_DURATION_MINUTES = 15;

    private final WaitlistRepository waitlistRepository;
    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final ZoneId platformTimeZone;

    public WaitlistService(
            WaitlistRepository waitlistRepository,
            BookingRepository bookingRepository,
            EquipmentRepository equipmentRepository,
            UserRepository userRepository,
            ZoneId platformTimeZone) {

        this.waitlistRepository = waitlistRepository;
        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.platformTimeZone = platformTimeZone;
    }

    // Convert platform-local time into UTC.
    private LocalDateTime toUtc(LocalDateTime platformTime) {
        return platformTime
                .atZone(platformTimeZone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    // Check whether two time intervals overlap.
    private boolean intervalsOverlap(
            LocalDateTime firstStart,
            LocalDateTime firstEnd,
            LocalDateTime secondStart,
            LocalDateTime secondEnd) {

        return firstStart.isBefore(secondEnd)
                && firstEnd.isAfter(secondStart);
    }

    // Join the waitlist.
    @Transactional
    public WaitlistResponseDTO joinWaitlist(
            Long userId,
            WaitlistRequestDTO request) {

        if (request.getEquipmentId() == null) {
            throw new IllegalArgumentException(
                    "Equipment ID is required");
        }

        LocalDateTime startTime = request.getRequestedStartTime();
        LocalDateTime endTime = request.getRequestedEndTime();

        LocalDateTime now =
                LocalDateTime.now(platformTimeZone);

        if (startTime == null || endTime == null
                || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "End time must be after start time");
        }

        if (!startTime.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Requested start time must be in the future");
        }

        if (request.getPurpose() == null
                || request.getPurpose().isBlank()
                || request.getPurpose().length() > 500) {
            throw new IllegalArgumentException(
                    "Purpose is required and must not exceed 500 characters");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Equipment equipment = equipmentRepository
                .findById(request.getEquipmentId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Equipment not found"));

        LocalDateTime utcStart = toUtc(startTime);
        LocalDateTime utcEnd = toUtc(endTime);

        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.APPROVED,
                BookingStatus.IN_USE
        );

        boolean overlapping =
                bookingRepository.existsOverlappingBooking(
                        equipment.getId(),
                        utcStart,
                        utcEnd,
                        blockingStatuses
                );

        if (!overlapping) {
            throw new IllegalArgumentException(
                    "Equipment is available. Please create a normal booking instead.");
        }

        List<WaitlistStatus> activeStatuses = List.of(
                WaitlistStatus.WAITING,
                WaitlistStatus.NOTIFIED
        );

        boolean alreadyWaiting =
                waitlistRepository
                        .existsByUser_IdAndEquipment_IdAndRequestedStartTimeAndRequestedEndTimeAndStatusIn(
                                userId,
                                equipment.getId(),
                                utcStart,
                                utcEnd,
                                activeStatuses
                        );

        if (alreadyWaiting) {
            throw new IllegalArgumentException(
                    "You already have an active waitlist request for this equipment");
        }

        Waitlist waitlist = new Waitlist();

        waitlist.setUser(user);
        waitlist.setEquipment(equipment);
        waitlist.setRequestedStartTime(utcStart);
        waitlist.setRequestedEndTime(utcEnd);
        waitlist.setPurpose(request.getPurpose());
        waitlist.setStatus(WaitlistStatus.WAITING);

        return new WaitlistResponseDTO(
                waitlistRepository.save(waitlist)
        );
    }

    // View a user's waitlist requests.
    @Transactional(readOnly = true)
    public List<WaitlistResponseDTO> getUserWaitlist(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        return waitlistRepository
                .findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(WaitlistResponseDTO::new)
                .toList();
    }

    // View the waitlist for particular equipment.
    @Transactional(readOnly = true)
    public List<WaitlistResponseDTO> getEquipmentWaitlist(
            Long equipmentId) {

        if (!equipmentRepository.existsById(equipmentId)) {
            throw new IllegalArgumentException("Equipment not found");
        }

        return waitlistRepository
                .findByEquipment_IdAndStatusOrderByCreatedAtAsc(
                        equipmentId,
                        WaitlistStatus.WAITING
                )
                .stream()
                .map(WaitlistResponseDTO::new)
                .toList();
    }

    // Offer an available slot to the first eligible waiting user.

    // Offer a released time interval to the first eligible waiting user.
    @Transactional
    public void offerNextWaitlistUser(
            Long equipmentId,
            LocalDateTime startTimeUtc,
            LocalDateTime endTimeUtc) {

        LocalDateTime now =
                LocalDateTime.now(ZoneOffset.UTC);

        if (equipmentId == null
                || startTimeUtc == null
                || endTimeUtc == null
                || !endTimeUtc.isAfter(startTimeUtc)
                || !endTimeUtc.isAfter(now)) {
            return;
        }

        // The released interval may begin at the current time.
        LocalDateTime availableStart =
                startTimeUtc.isAfter(now) ? startTimeUtc : now;

        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.APPROVED,
                BookingStatus.IN_USE
        );

        boolean overlappingBooking =
                bookingRepository.existsOverlappingBooking(
                        equipmentId,
                        availableStart,
                        endTimeUtc,
                        blockingStatuses
                );

        if (overlappingBooking) {
            return;
        }

        // Check existing active offers.
        List<Waitlist> notifiedUsers =
                waitlistRepository
                        .findByEquipment_IdAndStatusOrderByCreatedAtAsc(
                                equipmentId,
                                WaitlistStatus.NOTIFIED
                        );

        for (Waitlist existingOffer : notifiedUsers) {

            LocalDateTime offeredStart =
                    existingOffer.getOfferedStartTime();

            LocalDateTime offeredEnd =
                    existingOffer.getOfferedEndTime();

            boolean expired =
                    existingOffer.getOfferExpiresAt() == null
                            || !existingOffer.getOfferExpiresAt().isAfter(now);

            if (expired) {
                existingOffer.setStatus(WaitlistStatus.EXPIRED);
                waitlistRepository.save(existingOffer);
                continue;
            }

            if (offeredStart != null
                    && offeredEnd != null
                    && intervalsOverlap(
                    availableStart,
                    endTimeUtc,
                    offeredStart,
                    offeredEnd)) {
                return;
            }
        }

        // Get waiting users in FIFO order.
        List<Waitlist> waitingUsers =
                waitlistRepository
                        .findByEquipment_IdAndStatusOrderByCreatedAtAsc(
                                equipmentId,
                                WaitlistStatus.WAITING
                        );

        for (Waitlist nextUser : waitingUsers) {

            LocalDateTime requestedStart =
                    nextUser.getRequestedStartTime();

            LocalDateTime requestedEnd =
                    nextUser.getRequestedEndTime();

            // The entire requested interval must fit in the released interval.
            if (requestedStart == null
                    || requestedEnd == null
                    || requestedStart.isBefore(availableStart)
                    || requestedEnd.isAfter(endTimeUtc)
                    || !requestedEnd.isAfter(requestedStart)) {
                continue;
            }

            nextUser.setStatus(WaitlistStatus.NOTIFIED);
            nextUser.setOfferedAt(now);
            nextUser.setOfferExpiresAt(
                    now.plusMinutes(OFFER_DURATION_MINUTES)
            );

            nextUser.setOfferedStartTime(requestedStart);
            nextUser.setOfferedEndTime(requestedEnd);

            waitlistRepository.save(nextUser);

            // Only one user receives an offer at a time.
            return;
        }
    }


    // Accept an offered slot and create a pending booking.
    @Transactional
    public WaitlistResponseDTO acceptOffer(
            Long waitlistId,
            Long userId) {

        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Waitlist request not found"));

        if (!waitlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to accept this offer");
        }

        if (waitlist.getStatus() != WaitlistStatus.NOTIFIED) {
            throw new IllegalArgumentException(
                    "No active offer is available");
        }

        LocalDateTime now =
                LocalDateTime.now(ZoneOffset.UTC);

        LocalDateTime offeredStart =
                waitlist.getOfferedStartTime();

        LocalDateTime offeredEnd =
                waitlist.getOfferedEndTime();

        if (waitlist.getOfferExpiresAt() == null
                || !waitlist.getOfferExpiresAt().isAfter(now)
                || offeredStart == null
                || offeredEnd == null) {

            waitlist.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(waitlist);

            if (offeredStart != null && offeredEnd != null) {
                offerNextWaitlistUser(
                        waitlist.getEquipment().getId(),
                        offeredStart,
                        offeredEnd
                );
            }

            return new WaitlistResponseDTO(waitlist);
        }

        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.APPROVED,
                BookingStatus.IN_USE
        );

        boolean overlapping =
                bookingRepository.existsOverlappingBooking(
                        waitlist.getEquipment().getId(),
                        offeredStart,
                        offeredEnd,
                        blockingStatuses
                );

        if (overlapping) {

            waitlist.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(waitlist);

            offerNextWaitlistUser(
                    waitlist.getEquipment().getId(),
                    offeredStart,
                    offeredEnd
            );

            return new WaitlistResponseDTO(waitlist);
        }

        // Create the booking for the actual offered interval.
        Booking booking = new Booking();

        booking.setUser(waitlist.getUser());
        booking.setEquipment(waitlist.getEquipment());
        booking.setStartTime(offeredStart);
        booking.setEndTime(offeredEnd);
        booking.setPurpose(waitlist.getPurpose());
        booking.setUsageMode(UsageMode.EXCLUSIVE);
        booking.setStatus(BookingStatus.PENDING);

        bookingRepository.save(booking);

        waitlist.setStatus(WaitlistStatus.BOOKED);

        return new WaitlistResponseDTO(
                waitlistRepository.save(waitlist)
        );
    }

    // Decline an offer and offer the interval to the next user.
    @Transactional
    public WaitlistResponseDTO declineOffer(
            Long waitlistId,
            Long userId) {

        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Waitlist request not found"));

        if (!waitlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to decline this offer");
        }

        if (waitlist.getStatus() != WaitlistStatus.NOTIFIED) {
            throw new IllegalArgumentException(
                    "No active offer is available to decline");
        }

        LocalDateTime offeredStart =
                waitlist.getOfferedStartTime();

        LocalDateTime offeredEnd =
                waitlist.getOfferedEndTime();

        waitlist.setStatus(WaitlistStatus.DECLINED);

        Waitlist savedWaitlist =
                waitlistRepository.save(waitlist);

        if (offeredStart != null && offeredEnd != null) {
            offerNextWaitlistUser(
                    waitlist.getEquipment().getId(),
                    offeredStart,
                    offeredEnd
            );
        }

        return new WaitlistResponseDTO(savedWaitlist);
    }

    // Automatically expire offers and notify the next waiting user.
    @Transactional
    public int processExpiredOffers() {

        LocalDateTime now =
                LocalDateTime.now(ZoneOffset.UTC);

        List<Waitlist> expiredOffers =
                waitlistRepository
                        .findByStatusAndOfferExpiresAtLessThanEqual(
                                WaitlistStatus.NOTIFIED,
                                now
                        );

        for (Waitlist waitlist : expiredOffers) {

            Long equipmentId =
                    waitlist.getEquipment().getId();

            LocalDateTime offeredStart =
                    waitlist.getOfferedStartTime();

            LocalDateTime offeredEnd =
                    waitlist.getOfferedEndTime();

            waitlist.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(waitlist);

            if (offeredStart != null && offeredEnd != null) {
                offerNextWaitlistUser(
                        equipmentId,
                        offeredStart,
                        offeredEnd
                );
            }
        }

        return expiredOffers.size();
    }

    // Cancel a user's own waitlist request.
    @Transactional
    public WaitlistResponseDTO cancelWaitlist(
            Long waitlistId,
            Long userId) {

        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Waitlist request not found"));

        if (!waitlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to cancel this request");
        }

        if (waitlist.getStatus() != WaitlistStatus.WAITING
                && waitlist.getStatus() != WaitlistStatus.NOTIFIED) {
            throw new IllegalArgumentException(
                    "This waitlist request cannot be cancelled");
        }

        boolean wasNotified =
                waitlist.getStatus() == WaitlistStatus.NOTIFIED;

        LocalDateTime offeredStart =
                waitlist.getOfferedStartTime();

        LocalDateTime offeredEnd =
                waitlist.getOfferedEndTime();

        waitlist.setStatus(WaitlistStatus.CANCELLED);

        Waitlist savedWaitlist =
                waitlistRepository.save(waitlist);

        if (wasNotified
                && offeredStart != null
                && offeredEnd != null) {

            offerNextWaitlistUser(
                    waitlist.getEquipment().getId(),
                    offeredStart,
                    offeredEnd
            );
        }

        return new WaitlistResponseDTO(savedWaitlist);
    }
}
