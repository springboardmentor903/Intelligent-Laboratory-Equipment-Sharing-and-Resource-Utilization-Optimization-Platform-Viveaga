package com.labresource.platform.controller;

import com.labresource.platform.entity.Tag;
import com.labresource.platform.repository.TagRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagRepository tagRepository;

    public TagController(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    // Get all tags
    @GetMapping
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    // Create a tag
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tag createTag(@RequestBody Tag tag) {

        if (tag.getName() == null || tag.getName().isBlank()) {
            throw new IllegalArgumentException("Tag name is required");
        }

        if (tagRepository.existsByNameIgnoreCase(tag.getName())) {
            throw new IllegalArgumentException("Tag already exists");
        }

        return tagRepository.save(tag);
    }
}