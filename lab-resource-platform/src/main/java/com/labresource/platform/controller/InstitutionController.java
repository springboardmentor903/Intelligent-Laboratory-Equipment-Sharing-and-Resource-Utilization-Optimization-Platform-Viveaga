package com.labresource.platform.controller;

import com.labresource.platform.entity.Institution;
import com.labresource.platform.repository.InstitutionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {

    private final InstitutionRepository institutionRepository;

    public InstitutionController(
            InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    // Get all institutions
    @GetMapping
    public List<Institution> getAllInstitutions() {
        return institutionRepository.findAll();
    }

    // Create institution
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Institution createInstitution(
            @RequestBody Institution institution) {
        return institutionRepository.save(institution);
    }
}