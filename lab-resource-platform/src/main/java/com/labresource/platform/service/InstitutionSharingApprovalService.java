
package com.labresource.platform.service;

import com.labresource.platform.dto.InstitutionSharingRequestResponseDTO;
import com.labresource.platform.entity.*;
import com.labresource.platform.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InstitutionSharingApprovalService {

    private final InstitutionSharingRequestRepository requestRepository;
    private final EquipmentSharingRepository sharingRepository;
    private final SharingAgreementRepository agreementRepository;
    private final EquipmentAccessGrantRepository grantRepository;
    private final UserRepository userRepository;

    public InstitutionSharingApprovalService(
            InstitutionSharingRequestRepository requestRepository,
            EquipmentSharingRepository sharingRepository,
            SharingAgreementRepository agreementRepository,
            EquipmentAccessGrantRepository grantRepository,
            UserRepository userRepository
    ) {
        this.requestRepository = requestRepository;
        this.sharingRepository = sharingRepository;
        this.agreementRepository = agreementRepository;
        this.grantRepository = grantRepository;
        this.userRepository = userRepository;
    }

    // CREATE A NEW INSTITUTION SHARING REQUEST
    @Transactional
    public InstitutionSharingRequestResponseDTO createRequest(
            Long equipmentSharingId,
            String purpose,
            String email
    ) {
        User user = getInstitutionAdministrator(email);

        if (purpose == null || purpose.isBlank()) {
            throw new RuntimeException("Purpose is required");
        }

        EquipmentSharing sharing = sharingRepository
                .findById(equipmentSharingId)
                .orElseThrow(() ->
                        new RuntimeException("Equipment sharing not found"));

        if (sharing.getStatus() != SharingStatus.ACTIVE) {
            throw new RuntimeException(
                    "Equipment is not actively shared");
        }

        LocalDate today = LocalDate.now();

        if (today.isBefore(sharing.getStartDate())
                || today.isAfter(sharing.getEndDate())) {
            throw new RuntimeException(
                    "Equipment sharing is outside its valid dates");
        }

        Long partnerInstitutionId = user.getInstitution().getId();
        Long ownerInstitutionId = sharing.getInstitution().getId();

        if (partnerInstitutionId.equals(ownerInstitutionId)) {
            throw new RuntimeException(
                    "Your institution owns this equipment");
        }

        if (grantRepository
                .existsByEquipmentIdAndPartnerInstitutionIdAndActiveTrue(
                        sharing.getEquipment().getId(),
                        partnerInstitutionId
                )) {
            throw new RuntimeException(
                    "Your institution already has access to this equipment");
        }

        boolean pendingPartnerApproval =
                requestRepository
                        .existsByEquipmentSharingIdAndRequestingInstitutionIdAndStatus(
                                sharing.getId(),
                                partnerInstitutionId,
                                AgreementStatus.PENDING_PARTNER_APPROVAL
                        );

        boolean pendingOwnerApproval =
                requestRepository
                        .existsByEquipmentSharingIdAndRequestingInstitutionIdAndStatus(
                                sharing.getId(),
                                partnerInstitutionId,
                                AgreementStatus.PENDING_OWNER_APPROVAL
                        );

        if (pendingPartnerApproval || pendingOwnerApproval) {
            throw new RuntimeException(
                    "Your institution already has a pending sharing request");
        }

        InstitutionSharingRequest request =
                new InstitutionSharingRequest();

        request.setEquipmentSharing(sharing);
        request.setRequestingInstitution(user.getInstitution());
        request.setRequestedBy(user);
        request.setPurpose(purpose.trim());
        request.setStatus(AgreementStatus.PENDING_PARTNER_APPROVAL);
        request.setRequestedAt(LocalDateTime.now());

        InstitutionSharingRequest savedRequest =
                requestRepository.save(request);

        return toResponseDTO(savedRequest);
    }

    // GET REQUESTS WAITING FOR THE REQUESTING INSTITUTION'S APPROVAL
    @Transactional(readOnly = true)
    public List<InstitutionSharingRequestResponseDTO>
    getPartnerPendingRequests(String email) {

        User user = getInstitutionAdministrator(email);

        return requestRepository
                .findByRequestingInstitutionId(
                        user.getInstitution().getId()
                )
                .stream()
                .filter(request ->
                        request.getStatus()
                                == AgreementStatus.PENDING_PARTNER_APPROVAL)
                .map(this::toResponseDTO)
                .toList();
    }

    // GET ALL REQUESTS SUBMITTED BY THE LOGGED-IN INSTITUTION
    @Transactional(readOnly = true)
    public List<InstitutionSharingRequestResponseDTO>
    getMySubmittedRequests(String email) {

        User user = getInstitutionAdministrator(email);

        return requestRepository
                .findByRequestingInstitutionId(
                        user.getInstitution().getId()
                )
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // GET REQUESTS WAITING FOR THE EQUIPMENT OWNER'S APPROVAL
    @Transactional(readOnly = true)
    public List<InstitutionSharingRequestResponseDTO>
    getOwnerPendingRequests(String email) {

        User user = getInstitutionAdministrator(email);

        return requestRepository
                .findByEquipmentSharingInstitutionId(
                        user.getInstitution().getId()
                )
                .stream()
                .filter(request ->
                        request.getStatus()
                                == AgreementStatus.PENDING_OWNER_APPROVAL)
                .map(this::toResponseDTO)
                .toList();
    }

    // APPROVE REQUEST FROM THE REQUESTING INSTITUTION
    @Transactional
    public InstitutionSharingRequestResponseDTO approveByPartner(
            Long requestId,
            String email
    ) {
        User approver = getInstitutionAdministrator(email);

        InstitutionSharingRequest request = getRequest(requestId);

        Long partnerInstitutionId =
                request.getRequestingInstitution().getId();

        if (!partnerInstitutionId.equals(
                approver.getInstitution().getId())) {
            throw new RuntimeException(
                    "Only the requesting institution can approve this stage");
        }

        if (request.getStatus()
                != AgreementStatus.PENDING_PARTNER_APPROVAL) {
            throw new RuntimeException(
                    "Request is not waiting for partner approval");
        }

        request.setPartnerApprovedBy(approver);
        request.setPartnerApprovedAt(LocalDateTime.now());
        request.setStatus(AgreementStatus.PENDING_OWNER_APPROVAL);

        return toResponseDTO(requestRepository.save(request));
    }

    // FINAL APPROVAL FROM THE EQUIPMENT OWNER
    @Transactional
    public InstitutionSharingRequestResponseDTO approveByOwner(
            Long requestId,
            String email
    ) {
        User approver = getInstitutionAdministrator(email);

        InstitutionSharingRequest request = getRequest(requestId);

        EquipmentSharing sharing = request.getEquipmentSharing();

        Long ownerInstitutionId = sharing.getInstitution().getId();

        if (!ownerInstitutionId.equals(
                approver.getInstitution().getId())) {
            throw new RuntimeException(
                    "Only the equipment owner's institution can give final approval");
        }

        if (request.getStatus()
                != AgreementStatus.PENDING_OWNER_APPROVAL) {
            throw new RuntimeException(
                    "Request is not waiting for owner approval");
        }

        if (sharing.getStatus() != SharingStatus.ACTIVE) {
            throw new RuntimeException(
                    "Equipment sharing is no longer active");
        }

        if (sharing.getHourlyCost() == null
                || sharing.getHourlyCost() < 0) {
            throw new RuntimeException(
                    "A valid hourly sharing fee is required");
        }

        if (agreementRepository
                .findByInstitutionSharingRequestId(requestId)
                .isPresent()) {
            throw new RuntimeException(
                    "An agreement already exists for this request");
        }

        request.setOwnerApprovedBy(approver);
        request.setOwnerApprovedAt(LocalDateTime.now());
        request.setStatus(AgreementStatus.APPROVED);

        requestRepository.save(request);

        // CREATE SHARING AGREEMENT
        SharingAgreement agreement = new SharingAgreement();

        agreement.setInstitutionSharingRequest(request);
        agreement.setEquipmentSharing(sharing);
        agreement.setOwnerInstitution(sharing.getInstitution());
        agreement.setPartnerInstitution(
                request.getRequestingInstitution());

        agreement.setStartDate(sharing.getStartDate());
        agreement.setEndDate(sharing.getEndDate());

        agreement.setHourlyFee(
                BigDecimal.valueOf(sharing.getHourlyCost()));

        agreement.setMinimumAdvanceNoticeHours(0);
        agreement.setMaximumBookingDurationHours(24);
        agreement.setWeeklySharingLimitHours(
                new BigDecimal("40"));

        agreement.setStatus(AgreementStatus.APPROVED);

        SharingAgreement savedAgreement =
                agreementRepository.save(agreement);

        // CREATE EQUIPMENT ACCESS GRANT
        EquipmentAccessGrant grant = new EquipmentAccessGrant();

        grant.setAgreement(savedAgreement);
        grant.setEquipment(sharing.getEquipment());
        grant.setPartnerInstitution(
                request.getRequestingInstitution());

        grant.setValidFrom(sharing.getStartDate());
        grant.setValidUntil(sharing.getEndDate());
        grant.setActive(true);

        grantRepository.save(grant);

        return toResponseDTO(request);
    }

    // REJECT A PENDING REQUEST
    @Transactional
    public InstitutionSharingRequestResponseDTO rejectRequest(
            Long requestId,
            String email
    ) {
        User approver = getInstitutionAdministrator(email);

        InstitutionSharingRequest request = getRequest(requestId);

        Long approverInstitutionId =
                approver.getInstitution().getId();

        Long partnerInstitutionId =
                request.getRequestingInstitution().getId();

        Long ownerInstitutionId =
                request.getEquipmentSharing()
                        .getInstitution().getId();

        if (!approverInstitutionId.equals(partnerInstitutionId)
                && !approverInstitutionId.equals(ownerInstitutionId)) {
            throw new RuntimeException(
                    "You cannot reject this sharing request");
        }

        if (request.getStatus()
                != AgreementStatus.PENDING_PARTNER_APPROVAL
                && request.getStatus()
                != AgreementStatus.PENDING_OWNER_APPROVAL) {
            throw new RuntimeException(
                    "Only pending requests can be rejected");
        }

        request.setStatus(AgreementStatus.REJECTED);

        return toResponseDTO(requestRepository.save(request));
    }

    // FIND REQUEST BY ID
    private InstitutionSharingRequest getRequest(Long requestId) {

        return requestRepository.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Institution sharing request not found"));
    }

    // VALIDATE LOGGED-IN INSTITUTION ADMINISTRATOR
    private User getInstitutionAdministrator(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (user.getInstitution() == null) {
            throw new RuntimeException(
                    "User is not associated with an institution");
        }

        if (user.getRole() != Role.INSTITUTION_ADMINISTRATOR) {
            throw new RuntimeException(
                    "Only Institution Administrators can perform this action");
        }

        return user;
    }

    // CONVERT ENTITY TO RESPONSE DTO
    private InstitutionSharingRequestResponseDTO toResponseDTO(
            InstitutionSharingRequest request
    ) {
        EquipmentSharing sharing = request.getEquipmentSharing();

        InstitutionSharingRequestResponseDTO.InstitutionDTO requestingInstitution =
                new InstitutionSharingRequestResponseDTO.InstitutionDTO(
                        request.getRequestingInstitution().getName()
                );

        InstitutionSharingRequestResponseDTO.InstitutionDTO ownerInstitution =
                new InstitutionSharingRequestResponseDTO.InstitutionDTO(
                        sharing.getInstitution().getName()
                );

        InstitutionSharingRequestResponseDTO.EquipmentDTO equipment =
                new InstitutionSharingRequestResponseDTO.EquipmentDTO(
                        sharing.getEquipment().getName(),
                        sharing.getEquipment().getAssetTag()
                );

        InstitutionSharingRequestResponseDTO.EquipmentSharingDTO sharingDTO =
                new InstitutionSharingRequestResponseDTO.EquipmentSharingDTO(
                        equipment,
                        ownerInstitution,
                        sharing.getStartDate(),
                        sharing.getEndDate(),
                        sharing.getHourlyCost()
                );

        return new InstitutionSharingRequestResponseDTO(
                request.getId(),
                request.getPurpose(),
                request.getStatus(),
                request.getRequestedAt(),
                requestingInstitution,
                sharingDTO
        );
    }
}
