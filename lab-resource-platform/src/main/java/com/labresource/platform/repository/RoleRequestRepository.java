
package com.labresource.platform.repository;

import com.labresource.platform.entity.RoleRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRequestRepository
        extends JpaRepository<RoleRequest, Long> {

    List<RoleRequest> findByStatus(String status);
}
