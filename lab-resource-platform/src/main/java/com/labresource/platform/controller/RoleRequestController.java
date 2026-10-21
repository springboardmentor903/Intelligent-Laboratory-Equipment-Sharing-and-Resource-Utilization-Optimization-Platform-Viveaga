
package com.labresource.platform.controller;

import com.labresource.platform.dto.RoleRequestResponse;
import com.labresource.platform.entity.Role;
import com.labresource.platform.service.RoleRequestService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/role-requests")
public class RoleRequestController {

    private final RoleRequestService roleRequestService;

    public RoleRequestController(
            RoleRequestService roleRequestService) {
        this.roleRequestService = roleRequestService;
    }

    // Submit a role request
    @PostMapping
    public RoleRequestResponse createRequest(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        String roleValue = body.get("requestedRole");

        if (roleValue == null || roleValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Requested role is required");
        }

        Role requestedRole;

        try {
            requestedRole = Role.valueOf(roleValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid requested role");
        }

        return roleRequestService.createRequest(
                authentication.getName(),
                requestedRole
        );
    }

    // System Administrator views pending requests
    @GetMapping("/pending")
    public List<RoleRequestResponse> getPendingRequests() {
        return roleRequestService.getPendingRequests();
    }

    // System Administrator approves or rejects a request
    @PutMapping("/{id}/review")
    public RoleRequestResponse reviewRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {

        Boolean approved = body.get("approved");

        if (approved == null) {
            throw new IllegalArgumentException(
                    "The approved field must be true or false");
        }

        return roleRequestService.reviewRequest(id, approved);
    }
}
