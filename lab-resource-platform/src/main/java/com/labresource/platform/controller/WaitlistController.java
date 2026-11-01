
package com.labresource.platform.controller;

import com.labresource.platform.dto.WaitlistRequestDTO;
import com.labresource.platform.dto.WaitlistResponseDTO;
import com.labresource.platform.entity.User;
import com.labresource.platform.repository.UserRepository;
import com.labresource.platform.service.WaitlistService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final UserRepository userRepository;

    public WaitlistController(
            WaitlistService waitlistService,
            UserRepository userRepository) {
        this.waitlistService = waitlistService;
        this.userRepository = userRepository;
    }

    private Long getUserId(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return user.getId();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WaitlistResponseDTO joinWaitlist(
            @RequestBody WaitlistRequestDTO request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return waitlistService.joinWaitlist(userId, request);
    }

    @GetMapping("/my")
    public List<WaitlistResponseDTO> getMyWaitlist(
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return waitlistService.getUserWaitlist(userId);
    }

    @GetMapping("/equipment/{equipmentId}")
    public List<WaitlistResponseDTO> getEquipmentWaitlist(
            @PathVariable Long equipmentId) {

        return waitlistService.getEquipmentWaitlist(equipmentId);
    }

    @PutMapping("/{waitlistId}/cancel")
    public WaitlistResponseDTO cancelWaitlist(
            @PathVariable Long waitlistId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return waitlistService.cancelWaitlist(waitlistId, userId);
    }
}
