
package com.labresource.platform.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "waitlists",
        indexes = {
                @Index(
                        name = "idx_waitlist_equipment_status",
                        columnList = "equipment_id, status, created_at"
                ),
                @Index(
                        name = "idx_waitlist_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_waitlist_offer_expiry",
                        columnList = "status, offer_expires_at"
                )
        }
)
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "requested_start_time", nullable = false)
    private LocalDateTime requestedStartTime;

    @Column(name = "requested_end_time", nullable = false)
    private LocalDateTime requestedEndTime;

    @Column(nullable = false, length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Time when the system offered the slot.
    @Column(name = "offered_at")
    private LocalDateTime offeredAt;

    // Deadline for accepting the offer.
    @Column(name = "offer_expires_at")
    private LocalDateTime offerExpiresAt;

    // Actual start time of the slot being offered.
    @Column(name = "offered_start_time")
    private LocalDateTime offeredStartTime;

    // Actual end time of the slot being offered.
    @Column(name = "offered_end_time")
    private LocalDateTime offeredEndTime;

    public Waitlist() {
    }

    public Long getId() {
        return id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getRequestedStartTime() {
        return requestedStartTime;
    }

    public void setRequestedStartTime(LocalDateTime requestedStartTime) {
        this.requestedStartTime = requestedStartTime;
    }

    public LocalDateTime getRequestedEndTime() {
        return requestedEndTime;
    }

    public void setRequestedEndTime(LocalDateTime requestedEndTime) {
        this.requestedEndTime = requestedEndTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public void setStatus(WaitlistStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getOfferedAt() {
        return offeredAt;
    }

    public void setOfferedAt(LocalDateTime offeredAt) {
        this.offeredAt = offeredAt;
    }

    public LocalDateTime getOfferExpiresAt() {
        return offerExpiresAt;
    }

    public void setOfferExpiresAt(LocalDateTime offerExpiresAt) {
        this.offerExpiresAt = offerExpiresAt;
    }

    public LocalDateTime getOfferedStartTime() {
        return offeredStartTime;
    }

    public void setOfferedStartTime(LocalDateTime offeredStartTime) {
        this.offeredStartTime = offeredStartTime;
    }

    public LocalDateTime getOfferedEndTime() {
        return offeredEndTime;
    }

    public void setOfferedEndTime(LocalDateTime offeredEndTime) {
        this.offeredEndTime = offeredEndTime;
    }
}
