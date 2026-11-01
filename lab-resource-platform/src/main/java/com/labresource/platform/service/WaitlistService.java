package com.labresource.platform.service;

import com.labresource.platform.dto.WaitlistRequestDTO;
import com.labresource.platform.dto.WaitlistResponseDTO;
import com.labresource.platform.entity.*;
import com.labresource.platform.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    public WaitlistService(
            WaitlistRepository waitlistRepository,
            BookingRepository bookingRepository,
            EquipmentRepository equipmentRepository,
            UserRepository userRepository
    ) {
        this.waitlistRepository = waitlistRepository;
        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
    }

    // Add a student to the waitlist
    @Transactional
    public WaitlistResponseDTO joinWaitlist(
            Long userId,
            WaitlistRequestDTO request
    ) {

        if (request.getEquipmentId() == null) {
            throw new IllegalArgumentException("Equipment ID is required");
        }

        LocalDateTime startTime = request.getRequestedStartTime();
        LocalDateTime endTime = request.getRequestedEndTime();

        if (startTime == null || endTime == null
                || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }

        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Requested start time must be in the future"
            );
        }

        if (request.getPurpose() == null
                || request.getPurpose().isBlank()
                || request.getPurpose().length() > 500) {
            throw new IllegalArgumentException(
                    "Purpose is required and must not exceed 500 characters"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Equipment equipment = equipmentRepository
                .findById(request.getEquipmentId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Equipment not found"));

        // Prevent duplicate active waitlist requests
        List<WaitlistStatus> activeStatuses = List.of(
                WaitlistStatus.WAITING,
                WaitlistStatus.NOTIFIED
        );

        boolean alreadyWaiting =
                waitlistRepository
                        .existsByUser_IdAndEquipment_IdAndStatusIn(
                                userId,
                                equipment.getId(),
                                activeStatuses
                        );

        if (alreadyWaiting) {
            throw new IllegalArgumentException(
                    "You already have an active waitlist request for this equipment"
            );
        }

        // Check whether another booking occupies the requested time
        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.APPROVED,
                BookingStatus.IN_USE
        );

        boolean overlapping =
                bookingRepository.existsOverlappingBooking(
                        equipment.getId(),
                        startTime,
                        endTime,
                        blockingStatuses
                );

        if (!overlapping) {
            throw new IllegalArgumentException(
                    "Equipment is available for this time. Please create a normal booking instead."
            );
        }

        Waitlist waitlist = new Waitlist();

        waitlist.setUser(user);
        waitlist.setEquipment(equipment);
        waitlist.setRequestedStartTime(startTime);
        waitlist.setRequestedEndTime(endTime);
        waitlist.setPurpose(request.getPurpose());
        waitlist.setStatus(WaitlistStatus.WAITING);

        Waitlist savedWaitlist = waitlistRepository.save(waitlist);

        return new WaitlistResponseDTO(savedWaitlist);
    }

    // View a user's waitlist requests
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

    // View the waitlist for particular equipment
    @Transactional(readOnly = true)
    public List<WaitlistResponseDTO> getEquipmentWaitlist(
            Long equipmentId
    ) {

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

    // Cancel a user's own waitlist request
    @Transactional
    public WaitlistResponseDTO cancelWaitlist(
            Long waitlistId,
            Long userId
    ) {

        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Waitlist request not found"));

        if (!waitlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to cancel this request"
            );
        }

        if (waitlist.getStatus() != WaitlistStatus.WAITING
                && waitlist.getStatus() != WaitlistStatus.NOTIFIED) {
            throw new IllegalArgumentException(
                    "This waitlist request cannot be cancelled"
            );
        }

        waitlist.setStatus(WaitlistStatus.CANCELLED);

        return new WaitlistResponseDTO(
                waitlistRepository.save(waitlist)
        );
    }
}