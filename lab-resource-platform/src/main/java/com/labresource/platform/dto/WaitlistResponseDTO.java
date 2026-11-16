
package com.labresource.platform.dto;

import com.labresource.platform.entity.Waitlist;
import com.labresource.platform.entity.WaitlistStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class WaitlistResponseDTO {

    private static final ZoneId PLATFORM_TIME_ZONE =
            ZoneId.of("Asia/Kolkata");

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private Long userId;
    private String userName;

    private LocalDateTime requestedStartTime;
    private LocalDateTime requestedEndTime;

    private String purpose;
    private WaitlistStatus status;
    private LocalDateTime createdAt;

    // Offer information
    private LocalDateTime offeredAt;
    private LocalDateTime offerExpiresAt;
    private LocalDateTime offeredStartTime;
    private LocalDateTime offeredEndTime;

    public WaitlistResponseDTO(Waitlist waitlist) {

        this.id = waitlist.getId();

        this.equipmentId = waitlist.getEquipment().getId();
        this.equipmentName = waitlist.getEquipment().getName();

        this.userId = waitlist.getUser().getId();
        this.userName = waitlist.getUser().getFullName();

        this.requestedStartTime =
                convertUtcToPlatformTime(
                        waitlist.getRequestedStartTime()
                );

        this.requestedEndTime =
                convertUtcToPlatformTime(
                        waitlist.getRequestedEndTime()
                );

        this.purpose = waitlist.getPurpose();
        this.status = waitlist.getStatus();

        // CreatedAt is retained in its existing representation.
        this.createdAt = waitlist.getCreatedAt();

        this.offeredAt =
                convertUtcToPlatformTime(
                        waitlist.getOfferedAt()
                );

        this.offerExpiresAt =
                convertUtcToPlatformTime(
                        waitlist.getOfferExpiresAt()
                );

        this.offeredStartTime =
                convertUtcToPlatformTime(
                        waitlist.getOfferedStartTime()
                );

        this.offeredEndTime =
                convertUtcToPlatformTime(
                        waitlist.getOfferedEndTime()
                );
    }

    // Convert UTC timestamps into platform-local time.
    private static LocalDateTime convertUtcToPlatformTime(
            LocalDateTime utcTime) {

        if (utcTime == null) {
            return null;
        }

        return utcTime.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(PLATFORM_TIME_ZONE)
                .toLocalDateTime();
    }

    public Long getId() {
        return id;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public LocalDateTime getRequestedStartTime() {
        return requestedStartTime;
    }

    public LocalDateTime getRequestedEndTime() {
        return requestedEndTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getOfferedAt() {
        return offeredAt;
    }

    public LocalDateTime getOfferExpiresAt() {
        return offerExpiresAt;
    }

    public LocalDateTime getOfferedStartTime() {
        return offeredStartTime;
    }

    public LocalDateTime getOfferedEndTime() {
        return offeredEndTime;
    }
}
