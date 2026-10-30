package com.labresource.platform.controller;

import com.labresource.platform.entity.Department;
import com.labresource.platform.entity.Institution;
import com.labresource.platform.repository.DepartmentRepository;
import com.labresource.platform.repository.InstitutionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final InstitutionRepository institutionRepository;

    public DepartmentController(
            DepartmentRepository departmentRepository,
            InstitutionRepository institutionRepository) {
        this.departmentRepository = departmentRepository;
        this.institutionRepository = institutionRepository;
    }

    // Get all departments
    @GetMapping
    public List<Map<String, Object>> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(department -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", department.getId());
                    response.put("name", department.getName());
                    response.put("institutionId",
                            department.getInstitution().getId());
                    return response;
                })
                .toList();
    }

    // Create department
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Department createDepartment(
            @RequestBody Department department) {

        if (department.getInstitution() == null ||
                department.getInstitution().getId() == null) {
            throw new IllegalArgumentException(
                    "Institution ID is required");
        }

        Long institutionId = department.getInstitution().getId();

        Institution institution = institutionRepository
                .findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Institution not found: " + institutionId));

        department.setInstitution(institution);

        return departmentRepository.save(department);
    }
}