
package com.labresource.platform.repository;

import com.labresource.platform.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    List<Department> findByInstitution_Id(Long institutionId);

    boolean existsByNameIgnoreCaseAndInstitution_Id(
            String name, Long institutionId);
}
