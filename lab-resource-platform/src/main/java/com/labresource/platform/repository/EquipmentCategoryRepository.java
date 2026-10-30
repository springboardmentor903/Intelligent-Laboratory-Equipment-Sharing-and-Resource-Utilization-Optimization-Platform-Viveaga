
package com.labresource.platform.repository;

import com.labresource.platform.entity.EquipmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentCategoryRepository
        extends JpaRepository<EquipmentCategory, Long> {

    boolean existsByNameIgnoreCase(String name);
}
