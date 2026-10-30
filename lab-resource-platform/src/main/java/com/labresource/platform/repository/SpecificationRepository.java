
package com.labresource.platform.repository;

import com.labresource.platform.entity.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecificationRepository
        extends JpaRepository<Specification, Long> {

    List<Specification> findByEquipment_Id(Long equipmentId);
}
