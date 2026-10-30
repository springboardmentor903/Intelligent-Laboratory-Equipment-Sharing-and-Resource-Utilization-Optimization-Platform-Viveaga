package com.labresource.platform.repository;

import com.labresource.platform.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository
        extends JpaRepository<Institution, Long> {
}