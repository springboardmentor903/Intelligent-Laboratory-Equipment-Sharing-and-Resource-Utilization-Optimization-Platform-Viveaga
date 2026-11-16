
package com.labresource.platform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sharing_access_requests",
        indexes = {
                @Index(name = "idx_access_request_sharing", columnList = "equipment_sharing_id"),
                @Index(name = "idx_access_request_institution", columnList = "requesting_institution_id")
        }
)
public class SharingAccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sharing agreement for which access is requested
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_sharing_id", nullable = false)
    private EquipmentSharing equipmentSharing;

    // Institution requesting access
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requesting_institution_id", nullable = false)
    private Institution requestingInstitution;

    // User who submitted the request
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @Column(length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SharingAccessRequestStatus status =
            SharingAccessRequestStatus.PENDING;

    private LocalDateTime requestedAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    // Getters and Setters

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

    public SharingAccessRequestStatus getStatus() {
        return status;
    }

    public void setStatus(SharingAccessRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}
