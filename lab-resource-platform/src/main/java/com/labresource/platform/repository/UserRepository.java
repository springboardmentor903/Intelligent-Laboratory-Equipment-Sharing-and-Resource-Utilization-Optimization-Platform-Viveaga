package com.labresource.platform.repository;

import com.labresource.platform.entity.User;
import com.labresource.platform.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByInstitution_IdAndRole(Long institutionId, Role role);

    // Find the global System Administrator
    Optional<User> findFirstByRoleOrderByIdAsc(Role role);

    // Find the Institution Administrator of a particular institution
    Optional<User> findFirstByInstitution_IdAndRoleOrderByIdAsc(
            Long institutionId,
            Role role
    );
}