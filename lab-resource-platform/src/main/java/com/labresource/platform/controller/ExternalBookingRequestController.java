
package com.labresource.platform.controller;

import com.labresource.platform.dto.ExternalBookingRequestDTO;
import com.labresource.platform.dto.ExternalBookingRequestResponseDTO;
import com.labresource.platform.service.ExternalBookingRequestService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sharing/external-bookings")
public class ExternalBookingRequestController {

    private final ExternalBookingRequestService requestService;

    public ExternalBookingRequestController(
            ExternalBookingRequestService requestService
    ) {
        this.requestService = requestService;
    }

    // Submit external booking request
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExternalBookingRequestResponseDTO createRequest(
            @RequestBody ExternalBookingRequestDTO request,
            Authentication authentication
    ) {
        return requestService.createRequest(
                request,
                authentication.getName()
        );
    }

    // View requests submitted by my institution
    @GetMapping("/my-requests")
    public List<ExternalBookingRequestResponseDTO> getMyRequests(
            Authentication authentication
    ) {
        return requestService.getMyRequests(
                authentication.getName()
        );
    }

    // View incoming requests as equipment owner
    @GetMapping("/incoming")
    public List<ExternalBookingRequestResponseDTO> getIncomingRequests(
            Authentication authentication
    ) {
        System.out.println("Authenticated email: " + authentication.getName());

        return requestService.getIncomingRequests(
                authentication.getName()
        );
    }

    // Approve external booking request
    @PutMapping("/{id}/approve")
    public ExternalBookingRequestResponseDTO approveRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return requestService.approveRequest(
                id,
                authentication.getName()
        );
    }

    // Reject external booking request
    @PutMapping("/{id}/reject")
    public ExternalBookingRequestResponseDTO rejectRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return requestService.rejectRequest(
                id,
                authentication.getName()
        );
    }
}
