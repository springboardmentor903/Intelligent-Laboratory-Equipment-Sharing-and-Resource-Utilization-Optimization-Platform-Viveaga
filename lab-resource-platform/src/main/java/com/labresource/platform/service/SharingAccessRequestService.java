
package com.labresource.platform.service;

import com.labresource.platform.dto.SharingAccessRequestDTO;
import com.labresource.platform.dto.SharingAccessRequestResponseDTO;
import com.labresource.platform.entity.*;
import com.labresource.platform.repository.EquipmentSharingRepository;
import com.labresource.platform.repository.SharingAccessRequestRepository;
import com.labresource.platform.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SharingAccessRequestService {

    private final SharingAccessRequestRepository requestRepository;
    private final EquipmentSharingRepository sharingRepository;
    private final UserRepository userRepository;

    public SharingAccessRequestService(
            SharingAccessRequestRepository requestRepository,
            EquipmentSharingRepository sharingRepository,
            UserRepository userRepository
    ) {
        this.requestRepository = requestRepository;
        this.sharingRepository = sharingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SharingAccessRequestResponseDTO createRequest(
            SharingAccessRequestDTO request,
            String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (user.getInstitution() == null) {
            throw new RuntimeException(
                    "User is not associated with an institution");
        }

        if (request.getEquipmentSharingId() == null) {
            throw new RuntimeException(
                    "Equipment sharing ID is required");
        }

        if (request.getPurpose() == null
                || request.getPurpose().isBlank()) {
            throw new RuntimeException("Purpose is required");
        }

        EquipmentSharing sharing = sharingRepository
                .findById(request.getEquipmentSharingId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Equipment sharing agreement not found"));

        if (sharing.getStatus() != SharingStatus.ACTIVE) {
            throw new RuntimeException(
                    "This equipment is not actively shared");
        }

        LocalDate today = LocalDate.now();

        if (today.isBefore(sharing.getStartDate())
                || today.isAfter(sharing.getEndDate())) {
            throw new RuntimeException(
                    "This sharing agreement is outside its valid period");
        }

        Long requestingInstitutionId =
                user.getInstitution().getId();

        Long ownerInstitutionId =
                sharing.getInstitution().getId();

        if (requestingInstitutionId.equals(ownerInstitutionId)) {
            throw new RuntimeException(
                    "You cannot request access to your own institution's equipment");
        }

        boolean alreadyRequested = requestRepository
                .existsByEquipmentSharingIdAndRequestingInstitutionIdAndStatus(
                        sharing.getId(),
                        requestingInstitutionId,
                        SharingAccessRequestStatus.PENDING
                );

        if (alreadyRequested) {
            throw new RuntimeException(
                    "Your institution already has a pending request for this equipment");
        }

        SharingAccessRequest accessRequest =
                new SharingAccessRequest();

        accessRequest.setEquipmentSharing(sharing);
        accessRequest.setRequestingInstitution(user.getInstitution());
        accessRequest.setRequestedBy(user);
        accessRequest.setPurpose(request.getPurpose().trim());
        accessRequest.setStatus(
                SharingAccessRequestStatus.PENDING);

        SharingAccessRequest saved =
                requestRepository.save(accessRequest);

        return mapToResponse(saved);
    }

    // View requests received by the user's institution
    @Transactional(readOnly = true)
    public List<SharingAccessRequestResponseDTO> getIncomingRequests(
            String email
    ) {

        User user = getAuthorizedReviewer(email);

        return requestRepository
                .findByEquipmentSharingInstitutionId(
                        user.getInstitution().getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Approve an incoming access request
    @Transactional
    public SharingAccessRequestResponseDTO approveRequest(
            Long requestId,
            String email
    ) {
        return reviewRequest(
                requestId,
                email,
                SharingAccessRequestStatus.APPROVED
        );
    }

    // Reject an incoming access request
    @Transactional
    public SharingAccessRequestResponseDTO rejectRequest(
            Long requestId,
            String email
    ) {
        return reviewRequest(
                requestId,
                email,
                SharingAccessRequestStatus.REJECTED
        );
    }

    private SharingAccessRequestResponseDTO reviewRequest(
            Long requestId,
            String email,
            SharingAccessRequestStatus newStatus
    ) {

        User reviewer = getAuthorizedReviewer(email);

        SharingAccessRequest request = requestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Access request not found"));

        Long ownerInstitutionId = request
                .getEquipmentSharing()
                .getInstitution()
                .getId();

        if (!ownerInstitutionId.equals(
                reviewer.getInstitution().getId())) {
            throw new RuntimeException(
                    "You cannot review requests belonging to another institution");
        }

        if (request.getStatus()
                != SharingAccessRequestStatus.PENDING) {
            throw new RuntimeException(
                    "Only pending requests can be reviewed");
        }

        request.setStatus(newStatus);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());

        SharingAccessRequest saved =
                requestRepository.save(request);

        return mapToResponse(saved);
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
                    "You do not have permission to review access requests");
        }

        return user;
    }

    private SharingAccessRequestResponseDTO mapToResponse(
            SharingAccessRequest request
    ) {

        SharingAccessRequestResponseDTO dto =
                new SharingAccessRequestResponseDTO();

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

        dto.setPurpose(request.getPurpose());
        dto.setStatus(request.getStatus());
        dto.setRequestedAt(request.getRequestedAt());
        dto.setReviewedAt(request.getReviewedAt());

        if (request.getReviewedBy() != null) {
            dto.setReviewedByName(
                    request.getReviewedBy().getFullName());
        }

        return dto;
    }
}
