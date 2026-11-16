
package com.labresource.platform.dto;

public class SystemAdminUserDTO {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private Long institutionId;
    private String institutionName;

    public SystemAdminUserDTO(
            Long id,
            String fullName,
            String email,
            String role,
            Long institutionId,
            String institutionName) {

        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }
}
