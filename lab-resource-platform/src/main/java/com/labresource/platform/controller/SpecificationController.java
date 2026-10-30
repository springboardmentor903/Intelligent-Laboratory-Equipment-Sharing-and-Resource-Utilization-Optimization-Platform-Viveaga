
package com.labresource.platform.controller;

import com.labresource.platform.entity.Equipment;
import com.labresource.platform.entity.Specification;
import com.labresource.platform.repository.EquipmentRepository;
import com.labresource.platform.repository.SpecificationRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment/{equipmentId}/specifications")
public class SpecificationController {

    private final SpecificationRepository specificationRepository;
    private final EquipmentRepository equipmentRepository;

    public SpecificationController(
            SpecificationRepository specificationRepository,
            EquipmentRepository equipmentRepository) {
        this.specificationRepository = specificationRepository;
        this.equipmentRepository = equipmentRepository;
    }

    // GET all specifications for an equipment
    @GetMapping
    public List<Specification> getSpecifications(
            @PathVariable Long equipmentId) {

        if (!equipmentRepository.existsById(equipmentId)) {
            throw new RuntimeException(
                    "Equipment not found with ID: " + equipmentId);
        }

        return specificationRepository.findByEquipment_Id(equipmentId);
    }

    // POST a new specification
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Specification createSpecification(
            @PathVariable Long equipmentId,
            @RequestBody Specification specification) {

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException(
                        "Equipment not found with ID: " + equipmentId));

        if (specification.getName() == null
                || specification.getName().isBlank()
                || specification.getValue() == null
                || specification.getValue().isBlank()) {
            throw new IllegalArgumentException(
                    "Specification name and value are required");
        }

        specification.setId(null);
        specification.setEquipment(equipment);

        return specificationRepository.save(specification);
    }

    // DELETE a specification
    @DeleteMapping("/{specificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSpecification(
            @PathVariable Long equipmentId,
            @PathVariable Long specificationId) {

        if (!equipmentRepository.existsById(equipmentId)) {
            throw new RuntimeException(
                    "Equipment not found with ID: " + equipmentId);
        }

        Specification specification = specificationRepository
                .findById(specificationId)
                .orElseThrow(() -> new RuntimeException(
                        "Specification not found with ID: "
                                + specificationId));

        if (!specification.getEquipment().getId().equals(equipmentId)) {
            throw new IllegalArgumentException(
                    "Specification does not belong to this equipment");
        }

        specificationRepository.delete(specification);
    }
}
