
package com.labresource.platform.service;

import com.labresource.platform.dto.ExternalBookingRequestDTO;
import com.labresource.platform.dto.ExternalBookingRequestResponseDTO;
import com.labresource.platform.entity.*;
import com.labresource.platform.repository.ExternalBookingRequestRepository;
import com.labresource.platform.repository.SharingAccessRequestRepository;
import com.labresource.platform.repository.EquipmentSharingRepository;
import com.labresource.platform.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExternalBookingRequestService {

    private final ExternalBookingRequestRepository requestRepository;
    private final SharingAccessRequestRepository accessRequestRepository;
    private final EquipmentSharingRepository sharingRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;

    public ExternalBookingRequestService(
            ExternalBookingRequestRepository requestRepository,
            SharingAccessRequestRepository accessRequestRepository,
            EquipmentSharingRepository sharingRepository,
            UserRepository userRepository,
            BookingService bookingService
    ) {
        this.requestRepository = requestRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.sharingRepository = sharingRepository;
        this.userRepository = userRepository;
        this.bookingService = bookingService;
    }

    // Submit an external booking request
    @Transactional
    public ExternalBookingRequestResponseDTO createRequest(
            ExternalBookingRequestDTO dto,
            String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (user.getInstitution() == null) {
            throw new RuntimeException(
                    "User is not associated with an institution");
        }

        if (dto.getEquipmentSharingId() == null) {
            throw new RuntimeException(
                    "Equipment sharing ID is required");
        }

        if (dto.getStartTime() == null
                || dto.getEndTime() == null
                || !dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new RuntimeException("Invalid booking time");
        }

        if (dto.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Booking start time must be in the future");
        }

        if (dto.getPurpose() == null
                || dto.getPurpose().isBlank()
                || dto.getPurpose().length() > 500) {
            throw new RuntimeException(
                    "Purpose is required and must not exceed 500 characters");
        }

        EquipmentSharing sharing = sharingRepository
                .findById(dto.getEquipmentSharingId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Sharing agreement not found"));

        validateSharingPeriod(sharing, dto.getStartTime(),
                dto.getEndTime());

        Long institutionId = user.getInstitution().getId();

        if (institutionId.equals(sharing.getInstitution().getId())) {
            throw new RuntimeException(
                    "You cannot externally book your own institution's equipment");
        }

        boolean accessApproved = accessRequestRepository
                .existsByEquipmentSharingIdAndRequestingInstitutionIdAndStatus(
                        sharing.getId(),
                        institutionId,
                        SharingAccessRequestStatus.APPROVED
                );

        if (!accessApproved) {
            throw new RuntimeException(
                    "Your institution does not have approved sharing access");
        }

        ExternalBookingRequest request =
                new ExternalBookingRequest();

        request.setEquipmentSharing(sharing);
        request.setRequestingInstitution(user.getInstitution());
        request.setRequestedBy(user);
        request.setStartTime(dto.getStartTime());
        request.setEndTime(dto.getEndTime());
        request.setPurpose(dto.getPurpose().trim());
        request.setStatus(ExternalBookingRequestStatus.PENDING);

        return mapToResponse(requestRepository.save(request));
    }

    // View requests submitted by the user's institution
    @Transactional(readOnly = true)
    public List<ExternalBookingRequestResponseDTO> getMyRequests(
            String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (user.getInstitution() == null) {
            throw new RuntimeException(
                    "User is not associated with an institution");
        }

        return requestRepository
                .findByRequestingInstitutionId(
                        user.getInstitution().getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // View requests received by the equipment-owning institution
    @Transactional(readOnly = true)
    public List<ExternalBookingRequestResponseDTO> getIncomingRequests(
            String email
    ) {

        User reviewer = getAuthorizedReviewer(email);

        return requestRepository
                .findByEquipmentSharingInstitutionId(
                        reviewer.getInstitution().getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Approve an external booking request
    @Transactional
    public ExternalBookingRequestResponseDTO approveRequest(
            Long requestId,
            String email
    ) {

        User reviewer = getAuthorizedReviewer(email);

        ExternalBookingRequest request = requestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "External booking request not found"));

        validateOwner(request, reviewer);

        if (request.getStatus()
                != ExternalBookingRequestStatus.PENDING) {
            throw new RuntimeException(
                    "Only pending requests can be approved");
        }

        EquipmentSharing sharing = request.getEquipmentSharing();

        validateSharingPeriod(
                sharing,
                request.getStartTime(),
                request.getEndTime()
        );

        boolean accessApproved = accessRequestRepository
                .existsByEquipmentSharingIdAndRequestingInstitutionIdAndStatus(
                        sharing.getId(),
                        request.getRequestingInstitution().getId(),
                        SharingAccessRequestStatus.APPROVED
                );

        if (!accessApproved) {
            throw new RuntimeException(
                    "The requesting institution does not have approved sharing access");
        }

        UsageMode usageMode =
                sharing.getEquipment().getUsageMode();

        if (usageMode == null) {
            usageMode = UsageMode.EXCLUSIVE;
        }

        // Reuse the existing booking validation and creation logic
        Booking booking = bookingService.createBooking(
                request.getRequestedBy().getId(),
                sharing.getEquipment().getId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getPurpose(),
                usageMode
        );

        request.setBooking(booking);
        request.setStatus(ExternalBookingRequestStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());

        return mapToResponse(requestRepository.save(request));
    }

    // Reject an external booking request
    @Transactional
    public ExternalBookingRequestResponseDTO rejectRequest(
            Long requestId,
            String email
    ) {

        User reviewer = getAuthorizedReviewer(email);

        ExternalBookingRequest request = requestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "External booking request not found"));

        validateOwner(request, reviewer);

        if (request.getStatus()
                != ExternalBookingRequestStatus.PENDING) {
            throw new RuntimeException(
                    "Only pending requests can be rejected");
        }

        request.setStatus(ExternalBookingRequestStatus.REJECTED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());

        return mapToResponse(requestRepository.save(request));
    }

    private void validateSharingPeriod(
            EquipmentSharing sharing,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {

        if (sharing.getStatus() != SharingStatus.ACTIVE) {
            throw new RuntimeException(
                    "Sharing agreement is not active");
        }

        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        if (startDate.isBefore(sharing.getStartDate())
                || endDate.isAfter(sharing.getEndDate())) {
            throw new RuntimeException(
                    "Booking must fall within the sharing agreement period");
        }
    }

    private User getAuthorizedReviewer(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (user.getInstitution() == null) {
            throw new RuntimeException(
                    "User is not associated with an institution");
        }

        Role role = user.getRole();

        if (role != Role.LAB_MANAGER
                && role != Role.DEPARTMENT_HEAD
                && role != Role.INSTITUTION_ADMINISTRATOR) {
            throw new RuntimeException(
                    "You do not have permission to review external bookings");
        }

        return user;
    }

    private void validateOwner(
            ExternalBookingRequest request,
            User reviewer
    ) {

        Long ownerInstitutionId = request
                .getEquipmentSharing()
                .getInstitution()
                .getId();

        if (!ownerInstitutionId.equals(
                reviewer.getInstitution().getId())) {
            throw new RuntimeException(
                    "You cannot review another institution's booking request");
        }
    }

    private ExternalBookingRequestResponseDTO mapToResponse(
            ExternalBookingRequest request
    ) {

        ExternalBookingRequestResponseDTO dto =
                new ExternalBookingRequestResponseDTO();

        dto.setId(request.getId());

        dto.setEquipmentSharingId(
                request.getEquipmentSharing().getId());

        dto.setEquipmentName(
                request.getEquipmentSharing()
                        .getEquipment().getName());

        dto.setRequestingInstitutionId(
                request.getRequestingInstitution().getId());

        dto.setRequestingInstitutionName(
                request.getRequestingInstitution().getName());

        dto.setRequestedByUserId(
                request.getRequestedBy().getId());

        dto.setRequestedByName(
                request.getRequestedBy().getFullName());

        dto.setStartTime(request.getStartTime());
        dto.setEndTime(request.getEndTime());
        dto.setPurpose(request.getPurpose());
        dto.setStatus(request.getStatus());

        if (request.getBooking() != null) {
            dto.setBookingId(request.getBooking().getId());
        }

        dto.setRequestedAt(request.getRequestedAt());
        dto.setReviewedAt(request.getReviewedAt());

        if (request.getReviewedBy() != null) {
            dto.setReviewedByName(
                    request.getReviewedBy().getFullName());
        }

        return dto;
    }
}
