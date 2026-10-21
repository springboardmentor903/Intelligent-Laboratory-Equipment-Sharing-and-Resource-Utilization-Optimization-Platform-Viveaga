
package com.labresource.platform.dto;

import java.time.LocalDateTime;

public class RoleRequestResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String requestedRole;
    private String status;
    private LocalDateTime createdAt;

    public RoleRequestResponse(
            Long id,
            Long userId,
            String fullName,
            String email,
            String requestedRole,
            String status,
            LocalDateTime createdAt) {

        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.requestedRole = requestedRole;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRequestedRole() {
        return requestedRole;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
