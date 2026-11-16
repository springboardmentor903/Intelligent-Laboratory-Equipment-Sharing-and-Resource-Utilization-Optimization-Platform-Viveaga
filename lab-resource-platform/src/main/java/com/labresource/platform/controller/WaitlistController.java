
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

    // Get authenticated user ID.
    private Long getUserId(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Login required");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        return user.getId();
    }

    // Join waitlist.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WaitlistResponseDTO joinWaitlist(
            @RequestBody WaitlistRequestDTO request,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return waitlistService.joinWaitlist(userId, request);
    }

    // View my waitlist.
    @GetMapping("/my")
    public List<WaitlistResponseDTO> getMyWaitlist(
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return waitlistService.getUserWaitlist(userId);
    }

    // View equipment waitlist.
    @GetMapping("/equipment/{equipmentId}")
    public List<WaitlistResponseDTO> getEquipmentWaitlist(
            @PathVariable Long equipmentId,
            Authentication authentication) {

        getUserId(authentication);

        return waitlistService.getEquipmentWaitlist(equipmentId);
    }

    // Accept an available slot offer.
    @PostMapping("/{waitlistId}/accept")
    public WaitlistResponseDTO acceptOffer(
            @PathVariable Long waitlistId,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return waitlistService.acceptOffer(waitlistId, userId);
    }

    // Decline an available slot offer.
    @PostMapping("/{waitlistId}/decline")
    public WaitlistResponseDTO declineOffer(
            @PathVariable Long waitlistId,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return waitlistService.declineOffer(waitlistId, userId);
    }

    // Cancel my waitlist request.
    @PutMapping("/{waitlistId}/cancel")
    public WaitlistResponseDTO cancelWaitlist(
            @PathVariable Long waitlistId,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return waitlistService.cancelWaitlist(waitlistId, userId);
    }
}
