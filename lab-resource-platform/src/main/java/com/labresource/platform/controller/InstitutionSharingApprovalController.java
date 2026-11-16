
package com.labresource.platform.controller;

import com.labresource.platform.dto.InstitutionSharingRequestResponseDTO;
import com.labresource.platform.service.InstitutionSharingApprovalService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sharing/institution-requests")
public class InstitutionSharingApprovalController {

    private final InstitutionSharingApprovalService approvalService;

    public InstitutionSharingApprovalController(
            InstitutionSharingApprovalService approvalService
    ) {
        this.approvalService = approvalService;
    }

    // CREATE A NEW INSTITUTION SHARING REQUEST
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstitutionSharingRequestResponseDTO createRequest(
            @RequestParam Long equipmentSharingId,
            @RequestParam String purpose,
            Authentication authentication
    ) {
        return approvalService.createRequest(
                equipmentSharingId,
                purpose,
                authentication.getName()
        );
    }

    // GET REQUESTS WAITING FOR PARTNER INSTITUTION APPROVAL
    @GetMapping("/partner/pending")
    public List<InstitutionSharingRequestResponseDTO> getPartnerPendingRequests(
            Authentication authentication
    ) {
        return approvalService.getPartnerPendingRequests(
                authentication.getName()
        );
    }

    // GET ALL REQUESTS SUBMITTED BY THE LOGGED-IN INSTITUTION
    @GetMapping("/my-requests")
    public List<InstitutionSharingRequestResponseDTO> getMySubmittedRequests(
            Authentication authentication
    ) {
        return approvalService.getMySubmittedRequests(
                authentication.getName()
        );
    }

    // GET REQUESTS WAITING FOR EQUIPMENT OWNER APPROVAL
    @GetMapping("/owner/pending")
    public List<InstitutionSharingRequestResponseDTO> getOwnerPendingRequests(
            Authentication authentication
    ) {
        return approvalService.getOwnerPendingRequests(
                authentication.getName()
        );
    }

    // APPROVE REQUEST BY PARTNER INSTITUTION
    @PutMapping("/{id}/partner-approve")
    public InstitutionSharingRequestResponseDTO approveByPartner(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return approvalService.approveByPartner(
                id,
                authentication.getName()
        );
    }

    // APPROVE REQUEST BY EQUIPMENT OWNER
    @PutMapping("/{id}/owner-approve")
    public InstitutionSharingRequestResponseDTO approveByOwner(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return approvalService.approveByOwner(
                id,
                authentication.getName()
        );
    }

    // REJECT A PENDING REQUEST
    @PutMapping("/{id}/reject")
    public InstitutionSharingRequestResponseDTO rejectRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return approvalService.rejectRequest(
                id,
                authentication.getName()
        );
    }
}
