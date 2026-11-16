
package com.labresource.platform.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "equipment_access_grants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_grant_agreement",
                columnNames = {"agreement_id"}
        )
)
public class EquipmentAccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private SharingAgreement agreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_institution_id", nullable = false)
    private Institution partnerInstitution;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validUntil;

    @Column(nullable = false)
    private boolean active = true;

    public EquipmentAccessGrant() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SharingAgreement getAgreement() { return agreement; }
    public void setAgreement(SharingAgreement agreement) {
        this.agreement = agreement;
    }

    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public Institution getPartnerInstitution() { return partnerInstitution; }
    public void setPartnerInstitution(Institution partnerInstitution) {
        this.partnerInstitution = partnerInstitution;
    }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) {
        this.active = active;
    }
}
