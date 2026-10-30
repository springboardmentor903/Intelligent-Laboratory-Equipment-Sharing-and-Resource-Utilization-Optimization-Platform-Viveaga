package com.labresource.platform.controller;

import com.labresource.platform.entity.EquipmentCategory;
import com.labresource.platform.repository.EquipmentCategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment-categories")
public class EquipmentCategoryController {

    private final EquipmentCategoryRepository categoryRepository;

    public EquipmentCategoryController(
            EquipmentCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // Get all categories
    @GetMapping
    public List<EquipmentCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    // Create a category
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentCategory createCategory(
            @RequestBody EquipmentCategory category) {

        if (category.getName() == null ||
                category.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Category name is required");
        }

        if (categoryRepository.existsByNameIgnoreCase(
                category.getName())) {
            throw new IllegalArgumentException(
                    "Category already exists");
        }

        return categoryRepository.save(category);
    }
}