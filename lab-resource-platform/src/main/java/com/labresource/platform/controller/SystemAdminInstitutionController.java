
package com.labresource.platform.controller;

import com.labresource.platform.dto.InstitutionOverviewDTO;
import com.labresource.platform.dto.SystemAdminUserDTO;
import com.labresource.platform.service.SystemAdminInstitutionService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/system-admin")
public class SystemAdminInstitutionController {

    private final SystemAdminInstitutionService institutionService;

    public SystemAdminInstitutionController(
            SystemAdminInstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping("/institutions")
    public List<InstitutionOverviewDTO> getAllInstitutions(
            Authentication authentication) {

        boolean isSystemAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_SYSTEM_ADMINISTRATOR")
                                || authority.getAuthority().equals("SYSTEM_ADMINISTRATOR"));

        if (!isSystemAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only System Administrators can view all institutions"
            );
        }

        return institutionService.getAllInstitutions();
    }


    @GetMapping("/users")
    public List<SystemAdminUserDTO> getAllUsers(
            Authentication authentication) {

        boolean isSystemAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_SYSTEM_ADMINISTRATOR")
                                || authority.getAuthority().equals("SYSTEM_ADMINISTRATOR"));

        if (!isSystemAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only System Administrators can view all users"
            );
        }

        return institutionService.getAllUsers();
    }

}
