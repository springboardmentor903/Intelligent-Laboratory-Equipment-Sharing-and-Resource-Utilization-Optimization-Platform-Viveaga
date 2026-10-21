
package com.labresource.platform.service;

import com.labresource.platform.dto.RoleRequestResponse;
import com.labresource.platform.entity.Role;
import com.labresource.platform.entity.RoleRequest;
import com.labresource.platform.entity.User;
import com.labresource.platform.repository.RoleRequestRepository;
import com.labresource.platform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleRequestService {

    private final RoleRequestRepository roleRequestRepository;
    private final UserRepository userRepository;

    public RoleRequestService(
            RoleRequestRepository roleRequestRepository,
            UserRepository userRepository) {
        this.roleRequestRepository = roleRequestRepository;
        this.userRepository = userRepository;
    }

    public RoleRequestResponse createRequest(
            String email, Role requestedRole) {

        if (requestedRole == null) {
            throw new RuntimeException("Requested role is required");
        }

        if (requestedRole == Role.SYSTEM_ADMINISTRATOR) {
            throw new RuntimeException(
                    "System Administrator role cannot be requested");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (user.getRole() == requestedRole) {
            throw new RuntimeException("You already have this role");
        }

        RoleRequest request = new RoleRequest();
        request.setUser(user);
        request.setRequestedRole(requestedRole);
        request.setStatus("PENDING");

        return toResponse(roleRequestRepository.save(request));
    }

    public List<RoleRequestResponse> getPendingRequests() {
        return roleRequestRepository.findByStatus("PENDING")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoleRequestResponse reviewRequest(
            Long requestId, boolean approved) {

        RoleRequest request = roleRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException("Role request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException(
                    "Request has already been reviewed");
        }

        if (approved) {
            User user = request.getUser();
            user.setRole(request.getRequestedRole());
            userRepository.save(user);
            request.setStatus("APPROVED");
        } else {
            request.setStatus("REJECTED");
        }

        return toResponse(roleRequestRepository.save(request));
    }

    private RoleRequestResponse toResponse(RoleRequest request) {
        User user = request.getUser();

        return new RoleRequestResponse(
                request.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                request.getRequestedRole().name(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}
