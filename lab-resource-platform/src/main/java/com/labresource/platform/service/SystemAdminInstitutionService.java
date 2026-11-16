
package com.labresource.platform.service;

import com.labresource.platform.dto.InstitutionOverviewDTO;
import com.labresource.platform.dto.SystemAdminUserDTO;
import com.labresource.platform.entity.Institution;
import com.labresource.platform.entity.User;
import com.labresource.platform.repository.InstitutionRepository;
import com.labresource.platform.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SystemAdminInstitutionService {

    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;

    public SystemAdminInstitutionService(
            InstitutionRepository institutionRepository,
            UserRepository userRepository) {

        this.institutionRepository = institutionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<InstitutionOverviewDTO> getAllInstitutions() {

        List<Institution> institutions = institutionRepository.findAll();
        List<User> users = userRepository.findAll();

        List<InstitutionOverviewDTO> result = new ArrayList<>();

        for (Institution institution : institutions) {

            List<User> institutionUsers = users.stream()
                    .filter(user -> user.getInstitution() != null
                            && user.getInstitution().getId().equals(institution.getId()))
                    .collect(Collectors.toList());

            List<InstitutionOverviewDTO.StaffDTO> labManagers =
                    getStaffByRole(institutionUsers, "LAB_MANAGER");

            List<InstitutionOverviewDTO.StaffDTO> institutionAdmins =
                    getStaffByRole(institutionUsers, "INSTITUTION_ADMINISTRATOR");

            List<InstitutionOverviewDTO.StaffDTO> departmentHeads =
                    getStaffByRole(institutionUsers, "DEPARTMENT_HEAD");

            InstitutionOverviewDTO dto = new InstitutionOverviewDTO(
                    institution.getId(),
                    institution.getName(),
                    institution.getAddress(),
                    institutionUsers.size(),
                    labManagers,
                    institutionAdmins,
                    departmentHeads
            );

            result.add(dto);
        }

        return result;
    }

    private List<InstitutionOverviewDTO.StaffDTO> getStaffByRole(
            List<User> users, String roleName) {

        return users.stream()
                .filter(user -> user.getRole() != null
                        && user.getRole().name().equals(roleName))
                .map(user -> new InstitutionOverviewDTO.StaffDTO(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getRole().name()
                ))
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<SystemAdminUserDTO> getAllUsers() {

        List<User> users = userRepository.findAll();

        return users.stream()
                .map(user -> {
                    Long institutionId = null;
                    String institutionName = "Not Assigned";

                    if (user.getInstitution() != null) {
                        institutionId = user.getInstitution().getId();
                        institutionName = user.getInstitution().getName();
                    }

                    return new SystemAdminUserDTO(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getRole().name(),
                            institutionId,
                            institutionName
                    );
                })
                .collect(Collectors.toList());
    }

}
