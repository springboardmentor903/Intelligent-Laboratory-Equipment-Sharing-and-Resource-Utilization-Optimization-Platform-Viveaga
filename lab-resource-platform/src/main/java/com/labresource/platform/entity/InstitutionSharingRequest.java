
package com.labresource.platform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "institution_sharing_requests")
public class InstitutionSharingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_sharing_id", nullable = false)
    private EquipmentSharing equipmentSharing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_institution_id", nullable = false)
    private Institution requestingInstitution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Column(nullable = false, length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementStatus status = AgreementStatus.PENDING_PARTNER_APPROVAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_approved_by_id")
    private User partnerApprovedBy;

    private LocalDateTime partnerApprovedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_approved_by_id")
    private User ownerApprovedBy;

    private LocalDateTime ownerApprovedAt;

    private LocalDateTime requestedAt = LocalDateTime.now();

    public InstitutionSharingRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EquipmentSharing getEquipmentSharing() {
        return equipmentSharing;
    }

    public void setEquipmentSharing(EquipmentSharing equipmentSharing) {
        this.equipmentSharing = equipmentSharing;
    }

    public Institution getRequestingInstitution() {
        return requestingInstitution;
    }

    public void setRequestingInstitution(Institution requestingInstitution) {
        this.requestingInstitution = requestingInstitution;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public AgreementStatus getStatus() {
        return status;
    }

    public void setStatus(AgreementStatus status) {
        this.status = status;
    }

    public User getPartnerApprovedBy() {
        return partnerApprovedBy;
    }

    public void setPartnerApprovedBy(User partnerApprovedBy) {
        this.partnerApprovedBy = partnerApprovedBy;
    }

    public LocalDateTime getPartnerApprovedAt() {
        return partnerApprovedAt;
    }

    public void setPartnerApprovedAt(LocalDateTime partnerApprovedAt) {
        this.partnerApprovedAt = partnerApprovedAt;
    }

    public User getOwnerApprovedBy() {
        return ownerApprovedBy;
    }

    public void setOwnerApprovedBy(User ownerApprovedBy) {
        this.ownerApprovedBy = ownerApprovedBy;
    }

    public LocalDateTime getOwnerApprovedAt() {
        return ownerApprovedAt;
    }

    public void setOwnerApprovedAt(LocalDateTime ownerApprovedAt) {
        this.ownerApprovedAt = ownerApprovedAt;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
}
