
package com.labresource.platform.controller;

import com.labresource.platform.entity.Role;
import com.labresource.platform.entity.User;
import com.labresource.platform.repository.UserRepository;
import com.labresource.platform.service.SharingAnalyticsService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sharing/analytics")
public class SharingAnalyticsController {

    private final SharingAnalyticsService analyticsService;
    private final UserRepository userRepository;

    public SharingAnalyticsController(
            SharingAnalyticsService analyticsService,
            UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
    }

    private Long getInstitutionId(Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (user.getRole() == Role.SYSTEM_ADMINISTRATOR) {
            return null;
        }

        if (user.getRole() == Role.INSTITUTION_ADMINISTRATOR) {

            if (user.getInstitution() == null) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "User has no institution assigned");
            }

            return user.getInstitution().getId();
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only Institution Admin and System Admin can access sharing analytics");
    }

    // 1. Summary
    @GetMapping("/summary")
    public Map<String, Object> getSummary(
            Authentication authentication) {

        Long institutionId = getInstitutionId(authentication);

        return analyticsService.getSummary(institutionId);
    }

    // 2. Trend
    @GetMapping("/trend")
    public List<Map<String, Object>> getTrend(
            Authentication authentication) {

        Long institutionId = getInstitutionId(authentication);

        return analyticsService.getTrend(institutionId);
    }

    // 3. Partners
    @GetMapping("/partners")
    public List<Map<String, Object>> getPartners(
            Authentication authentication) {

        Long institutionId = getInstitutionId(authentication);

        return analyticsService.getPartners(institutionId);
    }

    // 4. Equipment
    @GetMapping("/equipment")
    public List<Map<String, Object>> getEquipment(
            Authentication authentication) {

        Long institutionId = getInstitutionId(authentication);

        return analyticsService.getEquipment(institutionId);
    }

    // 5. Partnership Report
    @GetMapping("/partners/{partnerId}/report")
    public Map<String, Object> getPartnerReport(
            @PathVariable Long partnerId,
            Authentication authentication) {

        Long institutionId = getInstitutionId(authentication);

        if (institutionId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "System Admin must select an institution before viewing a partnership report");
        }

        return analyticsService.getPartnerReport(
                institutionId, partnerId);
    }
}
