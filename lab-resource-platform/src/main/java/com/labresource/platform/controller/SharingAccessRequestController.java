
package com.labresource.platform.controller;

import com.labresource.platform.dto.SharingAccessRequestDTO;
import com.labresource.platform.dto.SharingAccessRequestResponseDTO;
import com.labresource.platform.service.SharingAccessRequestService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sharing/access-requests")
public class SharingAccessRequestController {

    private final SharingAccessRequestService requestService;

    public SharingAccessRequestController(
            SharingAccessRequestService requestService
    ) {
        this.requestService = requestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SharingAccessRequestResponseDTO createRequest(
            @RequestBody SharingAccessRequestDTO request,
            Authentication authentication
    ) {
        return requestService.createRequest(
                request,
                authentication.getName()
        );
    }

    @GetMapping("/incoming")
    public List<SharingAccessRequestResponseDTO> getIncomingRequests(
            Authentication authentication
    ) {
        return requestService.getIncomingRequests(
                authentication.getName()
        );
    }

    @PutMapping("/{id}/approve")
    public SharingAccessRequestResponseDTO approveRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return requestService.approveRequest(
                id,
                authentication.getName()
        );
    }

    @PutMapping("/{id}/reject")
    public SharingAccessRequestResponseDTO rejectRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return requestService.rejectRequest(
                id,
                authentication.getName()
        );
    }
}
