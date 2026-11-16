
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
import java.util.Objects;

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

    // Create request for an existing user
    @Transactional
    public RoleRequestResponse createRequest(
            String email,
            Role requestedRole) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return createRequestForUser(user, requestedRole);
    }

    // Create request during registration
    @Transactional
    public RoleRequestResponse createRequestForUser(
            User user,
            Role requestedRole) {

        if (requestedRole == null) {
            throw new RuntimeException("Requested role is required");
        }

        if (requestedRole == Role.SYSTEM_ADMINISTRATOR) {
            throw new RuntimeException(
                    "System Administrator role cannot be requested");
        }

        if (user.getRole() == requestedRole) {
            throw new RuntimeException("You already have this role");
        }

        if (user.getInstitution() == null) {
            throw new RuntimeException(
                    "User must belong to an institution");
        }

        User approver;

        // Institution Administrator requests go to System Administrator
        if (requestedRole == Role.INSTITUTION_ADMINISTRATOR) {

            approver = userRepository
                    .findFirstByRoleOrderByIdAsc(
                            Role.SYSTEM_ADMINISTRATOR)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "System Administrator not found"));
        }

        // Other requests go to their own Institution Administrator
        else {

            Long institutionId = user.getInstitution().getId();

            approver = userRepository
                    .findFirstByInstitution_IdAndRoleOrderByIdAsc(
                            institutionId,
                            Role.INSTITUTION_ADMINISTRATOR)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "No Institution Administrator found for this institution"));
        }

        RoleRequest request = new RoleRequest();

        request.setUser(user);
        request.setRequestedRole(requestedRole);
        request.setAssignedTo(approver);
        request.setStatus("PENDING");

        return toResponse(roleRequestRepository.save(request));
    }

    // Get pending requests for logged-in administrator
    @Transactional(readOnly = true)
    public List<RoleRequestResponse> getPendingRequests(String email) {

        User admin = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Administrator not found"));

        List<RoleRequest> requests =
                roleRequestRepository.findByStatus("PENDING");

        // System Administrator sees all pending requests
        if (admin.getRole() == Role.SYSTEM_ADMINISTRATOR) {

            return requests.stream()
                    .map(this::toResponse)
                    .toList();
        }

        // Institution Administrator sees own institution requests
        if (admin.getRole() == Role.INSTITUTION_ADMINISTRATOR) {

            if (admin.getInstitution() == null) {
                throw new RuntimeException(
                        "Institution Administrator has no institution");
            }

            Long institutionId = admin.getInstitution().getId();

            return requests.stream()

                    .filter(request ->
                            request.getUser().getInstitution() != null)

                    .filter(request ->
                            Objects.equals(
                                    request.getUser()
                                            .getInstitution().getId(),
                                    institutionId))

                    .filter(request ->
                            request.getRequestedRole()
                                    != Role.INSTITUTION_ADMINISTRATOR)

                    .filter(request ->
                            request.getAssignedTo() == null
                                    || Objects.equals(
                                    request.getAssignedTo().getId(),
                                    admin.getId()))

                    .map(this::toResponse)
                    .toList();
        }

        throw new RuntimeException(
                "You are not authorized to view role requests");
    }

    // Approve or reject request
    @Transactional
    public RoleRequestResponse reviewRequest(
            String email,
            Long requestId,
            boolean approved) {

        User admin = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Administrator not found"));

        RoleRequest request = roleRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException("Role request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException(
                    "This request has already been reviewed");
        }

        boolean authorized = false;

        // System Administrator can review only Institution Administrator requests
        if (admin.getRole() == Role.SYSTEM_ADMINISTRATOR) {

            authorized =
                    request.getRequestedRole()
                            == Role.INSTITUTION_ADMINISTRATOR;
        }

        // Institution Administrator can review requests from own institution
        else if (admin.getRole() == Role.INSTITUTION_ADMINISTRATOR) {

            if (admin.getInstitution() == null
                    || request.getUser().getInstitution() == null) {

                throw new RuntimeException(
                        "Institution information is missing");
            }

            Long adminInstitutionId =
                    admin.getInstitution().getId();

            Long requestInstitutionId =
                    request.getUser().getInstitution().getId();

            authorized =
                    Objects.equals(
                            adminInstitutionId,
                            requestInstitutionId)
                            &&
                            request.getRequestedRole()
                                    != Role.INSTITUTION_ADMINISTRATOR;
        }

        if (!authorized) {
            throw new RuntimeException(
                    "You are not authorized to review this request");
        }

        // Verify assigned administrator when assignment exists
        if (request.getAssignedTo() != null
                && !Objects.equals(
                request.getAssignedTo().getId(),
                admin.getId())) {

            throw new RuntimeException(
                    "This request is assigned to another administrator");
        }

        if (approved) {

            // Approve request
            User requestedUser = request.getUser();

            requestedUser.setRole(request.getRequestedRole());

            userRepository.save(requestedUser);

            request.setStatus("APPROVED");

        } else {

            // Reject request
            request.setStatus("REJECTED");
        }

        return toResponse(roleRequestRepository.save(request));
    }

    // Convert entity to response DTO
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
