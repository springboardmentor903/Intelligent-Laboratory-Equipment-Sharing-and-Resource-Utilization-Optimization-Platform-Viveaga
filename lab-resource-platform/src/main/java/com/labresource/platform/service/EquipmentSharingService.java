
package com.labresource.platform.service;

import com.labresource.platform.dto.EquipmentSharingRequestDTO;
import com.labresource.platform.dto.EquipmentSharingResponseDTO;
import com.labresource.platform.entity.*;
import com.labresource.platform.repository.EquipmentRepository;
import com.labresource.platform.repository.EquipmentSharingRepository;
import com.labresource.platform.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class EquipmentSharingService {

    private final EquipmentSharingRepository sharingRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    public EquipmentSharingService(
            EquipmentSharingRepository sharingRepository,
            EquipmentRepository equipmentRepository,
            UserRepository userRepository
    ) {
        this.sharingRepository = sharingRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EquipmentSharingResponseDTO publishEquipment(
            EquipmentSharingRequestDTO request,
            String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        validateManagerPermission(user);

        if (user.getInstitution() == null) {
            throw new RuntimeException(
                    "User is not associated with an institution");
        }

        if (request.getEquipmentId() == null) {
            throw new RuntimeException("Equipment ID is required");
        }

        if (request.getStartDate() == null
                || request.getEndDate() == null) {
            throw new RuntimeException(
                    "Sharing start date and end date are required");
        }

        if (request.getStartDate().isBefore(LocalDate.now())
                || !request.getEndDate().isAfter(request.getStartDate())) {
            throw new RuntimeException(
                    "Invalid sharing period");
        }

        if (request.getHourlyCost() != null
                && request.getHourlyCost() < 0) {
            throw new RuntimeException(
                    "Hourly cost cannot be negative");
        }

        Equipment equipment = equipmentRepository
                .findById(request.getEquipmentId())
                .orElseThrow(() ->
                        new RuntimeException("Equipment not found"));

        if (equipment.getDepartment() == null
                || equipment.getDepartment().getInstitution() == null) {
            throw new RuntimeException(
                    "Equipment ownership information is missing");
        }

        Long ownerInstitutionId = equipment.getDepartment()
                .getInstitution().getId();

        if (!ownerInstitutionId.equals(
                user.getInstitution().getId())) {
            throw new RuntimeException(
                    "You cannot share equipment owned by another institution");
        }

        boolean alreadyPublished = sharingRepository
                .findByEquipmentId(equipment.getId())
                .stream()
                .anyMatch(existing ->
                        existing.getStatus() == SharingStatus.ACTIVE
                                && !request.getEndDate().isBefore(
                                existing.getStartDate())
                                && !request.getStartDate().isAfter(
                                existing.getEndDate()));

        if (alreadyPublished) {
            throw new RuntimeException(
                    "Equipment already has an active sharing agreement for this period");
        }

        EquipmentSharing sharing = new EquipmentSharing();

        sharing.setEquipment(equipment);
        sharing.setInstitution(user.getInstitution());
        sharing.setStartDate(request.getStartDate());
        sharing.setEndDate(request.getEndDate());
        sharing.setSharingTerms(request.getSharingTerms());
        sharing.setHourlyCost(request.getHourlyCost());
        sharing.setStatus(SharingStatus.ACTIVE);

        EquipmentSharing saved = sharingRepository.save(sharing);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EquipmentSharingResponseDTO> getActiveSharedEquipment(
            String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (user.getInstitution() == null) {
            throw new RuntimeException(
                    "User is not associated with an institution");
        }

        Long currentInstitutionId = user.getInstitution().getId();

        LocalDate today = LocalDate.now();

        return sharingRepository.findByStatus(SharingStatus.ACTIVE)
                .stream()
                .filter(sharing ->
                        !sharing.getInstitution().getId()
                                .equals(currentInstitutionId))
                .filter(sharing ->
                        !today.isBefore(sharing.getStartDate())
                                && !today.isAfter(sharing.getEndDate()))
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EquipmentSharingResponseDTO> getMyInstitutionSharings(
            String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (user.getInstitution() == null) {
            throw new RuntimeException(
                    "User is not associated with an institution");
        }

        return sharingRepository
                .findByInstitutionId(user.getInstitution().getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateManagerPermission(User user) {

        Role role = user.getRole();

        if (role != Role.LAB_MANAGER
                && role != Role.DEPARTMENT_HEAD
                && role != Role.INSTITUTION_ADMINISTRATOR) {

            throw new RuntimeException(
                    "You do not have permission to publish equipment for sharing");
        }
    }

    private EquipmentSharingResponseDTO mapToResponse(
            EquipmentSharing sharing
    ) {

        EquipmentSharingResponseDTO dto =
                new EquipmentSharingResponseDTO();

        dto.setId(sharing.getId());

        dto.setEquipmentId(sharing.getEquipment().getId());
        dto.setEquipmentName(sharing.getEquipment().getName());
        dto.setAssetTag(sharing.getEquipment().getAssetTag());

        dto.setInstitutionId(sharing.getInstitution().getId());
        dto.setInstitutionName(sharing.getInstitution().getName());

        dto.setStartDate(sharing.getStartDate());
        dto.setEndDate(sharing.getEndDate());
        dto.setSharingTerms(sharing.getSharingTerms());
        dto.setHourlyCost(sharing.getHourlyCost());
        dto.setStatus(sharing.getStatus());
        dto.setCreatedAt(sharing.getCreatedAt());

        return dto;
    }
}
