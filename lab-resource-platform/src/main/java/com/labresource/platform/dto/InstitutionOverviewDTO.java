
package com.labresource.platform.dto;

import java.util.List;

public class InstitutionOverviewDTO {

    private Long id;
    private String name;
    private String address;
    private long totalUsers;

    private List<StaffDTO> labManagers;
    private List<StaffDTO> institutionAdmins;
    private List<StaffDTO> departmentHeads;

    public InstitutionOverviewDTO(
            Long id,
            String name,
            String address,
            long totalUsers,
            List<StaffDTO> labManagers,
            List<StaffDTO> institutionAdmins,
            List<StaffDTO> departmentHeads) {

        this.id = id;
        this.name = name;
        this.address = address;
        this.totalUsers = totalUsers;
        this.labManagers = labManagers;
        this.institutionAdmins = institutionAdmins;
        this.departmentHeads = departmentHeads;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public List<StaffDTO> getLabManagers() {
        return labManagers;
    }

    public List<StaffDTO> getInstitutionAdmins() {
        return institutionAdmins;
    }

    public List<StaffDTO> getDepartmentHeads() {
        return departmentHeads;
    }

    public static class StaffDTO {

        private Long id;
        private String fullName;
        private String email;
        private String role;

        public StaffDTO(Long id, String fullName, String email, String role) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
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
    }
}
