
package com.labresource.platform.service;

import com.labresource.platform.dto.EquipmentDetailDTO;
import com.labresource.platform.dto.EquipmentListDTO;
import com.labresource.platform.entity.Equipment;
import com.labresource.platform.entity.EquipmentStatus;
import com.labresource.platform.entity.Tag;
import com.labresource.platform.entity.User;
import com.labresource.platform.entity.Role;
import com.labresource.platform.repository.EquipmentRepository;
import com.labresource.platform.repository.TagRepository;
import com.labresource.platform.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    public EquipmentService(
            EquipmentRepository equipmentRepository,
            TagRepository tagRepository,
            UserRepository userRepository) {
        this.equipmentRepository = equipmentRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    public List<Equipment> getAllEquipment() {
        return equipmentRepository.findAll();
    }

    public EquipmentDetailDTO getEquipmentById(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Equipment not found with ID: " + id));

        return EquipmentDetailDTO.fromEntity(equipment);
    }

    public EquipmentDetailDTO getEquipmentByAssetTag(String assetTag) {
        Equipment equipment = equipmentRepository.findByAssetTag(assetTag)
                .orElseThrow(() -> new RuntimeException(
                        "Equipment not found with asset tag: " + assetTag));

        return EquipmentDetailDTO.fromEntity(equipment);
    }

    @Transactional
    public Equipment createEquipment(Equipment equipment) {

        if (equipment.getName() == null || equipment.getName().isBlank()) {
            throw new IllegalArgumentException("Equipment name is required");
        }

        if (equipment.getAssetTag() == null || equipment.getAssetTag().isBlank()) {
            throw new IllegalArgumentException("Asset tag is required");
        }

        if (equipmentRepository.existsByAssetTag(equipment.getAssetTag())) {
            throw new IllegalArgumentException(
                    "Asset tag already exists: " + equipment.getAssetTag());
        }

        equipment.setStatus(EquipmentStatus.AVAILABLE);

        return equipmentRepository.save(equipment);
    }

    // Institution-wise equipment search
    public Page<EquipmentListDTO> searchEquipment(
            int page,
            int size,
            EquipmentStatus status,
            Long categoryId,
            Long tagId,
            Long departmentId,
            String keyword,
            String email) {

        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Specification<Equipment> spec =
                (root, query, cb) -> cb.conjunction();

        // System Administrator can view all institutions.
        // Other users can view only their own institution's equipment.
        if (user.getRole() != Role.SYSTEM_ADMINISTRATOR) {

            if (user.getInstitution() == null) {
                throw new IllegalStateException(
                        "Your account is not associated with an institution");
            }

            Long institutionId = user.getInstitution().getId();

            spec = spec.and((root, query, cb) ->
                    cb.equal(
                            root.get("department")
                                    .get("institution")
                                    .get("id"),
                            institutionId
                    ));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status));
        }

        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }

        if (departmentId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("department").get("id"), departmentId));
        }

        if (tagId != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(root.join("tags").get("id"), tagId);
            });
        }

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), pattern),
                            cb.like(cb.lower(root.get("model")), pattern),
                            cb.like(cb.lower(root.get("manufacturer")), pattern),
                            cb.like(cb.lower(root.get("assetTag")), pattern)
                    ));
        }

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "id"));

        return equipmentRepository.findAll(spec, pageable)
                .map(EquipmentListDTO::fromEntity);
    }

    @Transactional
    public EquipmentDetailDTO updateEquipment(Long id, Equipment updated) {

        Equipment existing = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Equipment not found with ID: " + id));

        if (updated.getName() == null || updated.getName().isBlank()) {
            throw new IllegalArgumentException("Equipment name is required");
        }

        if (updated.getAssetTag() == null || updated.getAssetTag().isBlank()) {
            throw new IllegalArgumentException("Asset tag is required");
        }

        equipmentRepository.findByAssetTag(updated.getAssetTag())
                .filter(e -> !e.getId().equals(id))
                .ifPresent(e -> {
                    throw new IllegalArgumentException(
                            "Asset tag already exists: " + updated.getAssetTag());
                });

        existing.setName(updated.getName());
        existing.setModel(updated.getModel());
        existing.setManufacturer(updated.getManufacturer());
        existing.setSerialNumber(updated.getSerialNumber());
        existing.setAssetTag(updated.getAssetTag());
        existing.setLocation(updated.getLocation());
        existing.setPurchaseDate(updated.getPurchaseDate());
        existing.setPurchaseCost(updated.getPurchaseCost());
        existing.setHourlyCost(updated.getHourlyCost());
        existing.setCategory(updated.getCategory());
        existing.setDepartment(updated.getDepartment());

        Equipment saved = equipmentRepository.save(existing);

        return EquipmentDetailDTO.fromEntity(saved);
    }

    @Transactional
    public Equipment updateEquipmentStatus(Long id, EquipmentStatus newStatus) {

        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Equipment not found with ID: " + id));

        if (newStatus == null) {
            throw new IllegalArgumentException("Status is required");
        }

        if (equipment.getStatus() == EquipmentStatus.RETIRED) {
            throw new IllegalArgumentException(
                    "Retired equipment status cannot be changed");
        }

        if (newStatus == EquipmentStatus.BOOKED) {
            throw new IllegalArgumentException(
                    "BOOKED status cannot be set manually");
        }

        equipment.setStatus(newStatus);

        return equipmentRepository.save(equipment);
    }

    @Transactional
    public EquipmentDetailDTO assignTags(Long equipmentId, Set<Long> tagIds) {

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException(
                        "Equipment not found with ID: " + equipmentId));

        if (tagIds == null || tagIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one tag ID is required");
        }

        Set<Tag> tags = new HashSet<>(tagRepository.findAllById(tagIds));

        if (tags.size() != tagIds.size()) {
            throw new IllegalArgumentException(
                    "One or more tag IDs are invalid");
        }

        equipment.getTags().addAll(tags);
        equipmentRepository.save(equipment);

        return EquipmentDetailDTO.fromEntity(equipment);
    }
}
