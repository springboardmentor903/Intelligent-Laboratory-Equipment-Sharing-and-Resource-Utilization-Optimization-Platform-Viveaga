
package com.labresource.platform.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "sharing_agreements")
public class SharingAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_sharing_request_id",
            nullable = false, unique = true)
    private InstitutionSharingRequest institutionSharingRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_sharing_id", nullable = false)
    private EquipmentSharing equipmentSharing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_institution_id", nullable = false)
    private Institution ownerInstitution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_institution_id", nullable = false)
    private Institution partnerInstitution;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyFee;

    @Column(nullable = false)
    private Integer minimumAdvanceNoticeHours = 0;

    @Column(nullable = false)
    private Integer maximumBookingDurationHours = 24;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weeklySharingLimitHours = new BigDecimal("40");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementStatus status = AgreementStatus.APPROVED;

    public SharingAgreement() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InstitutionSharingRequest getInstitutionSharingRequest() {
        return institutionSharingRequest;
    }
    public void setInstitutionSharingRequest(InstitutionSharingRequest request) {
        this.institutionSharingRequest = request;
    }

    public EquipmentSharing getEquipmentSharing() { return equipmentSharing; }
    public void setEquipmentSharing(EquipmentSharing equipmentSharing) {
        this.equipmentSharing = equipmentSharing;
    }

    public Institution getOwnerInstitution() { return ownerInstitution; }
    public void setOwnerInstitution(Institution ownerInstitution) {
        this.ownerInstitution = ownerInstitution;
    }

    public Institution getPartnerInstitution() { return partnerInstitution; }
    public void setPartnerInstitution(Institution partnerInstitution) {
        this.partnerInstitution = partnerInstitution;
    }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getHourlyFee() { return hourlyFee; }
    public void setHourlyFee(BigDecimal hourlyFee) { this.hourlyFee = hourlyFee; }

    public Integer getMinimumAdvanceNoticeHours() {
        return minimumAdvanceNoticeHours;
    }
    public void setMinimumAdvanceNoticeHours(Integer hours) {
        this.minimumAdvanceNoticeHours = hours;
    }

    public Integer getMaximumBookingDurationHours() {
        return maximumBookingDurationHours;
    }
    public void setMaximumBookingDurationHours(Integer hours) {
        this.maximumBookingDurationHours = hours;
    }

    public BigDecimal getWeeklySharingLimitHours() {
        return weeklySharingLimitHours;
    }
    public void setWeeklySharingLimitHours(BigDecimal hours) {
        this.weeklySharingLimitHours = hours;
    }

    public AgreementStatus getStatus() { return status; }
    public void setStatus(AgreementStatus status) { this.status = status; }
}
