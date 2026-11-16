
package com.labresource.platform.dto;

import com.labresource.platform.entity.Booking;
import com.labresource.platform.entity.BookingStatus;
import com.labresource.platform.entity.UsageMode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class BookingResponseDTO {

    private static final ZoneId PLATFORM_TIME_ZONE =
            ZoneId.of("Asia/Kolkata");

    private Long id;
    private String recurringSeriesId;

    private Long userId;
    private String userName;
    private String userRole;

    private Long equipmentId;
    private String equipmentName;
    private String assetTag;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String purpose;
    private BookingStatus status;
    private UsageMode usageMode;
    private LocalDateTime createdAt;

    public BookingResponseDTO() {
    }

    private static LocalDateTime convertUtcToPlatformTime(
            LocalDateTime utcTime) {

        if (utcTime == null) {
            return null;
        }

        return utcTime.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(PLATFORM_TIME_ZONE)
                .toLocalDateTime();
    }

    public static BookingResponseDTO fromEntity(Booking booking) {

        BookingResponseDTO dto = new BookingResponseDTO();

        dto.id = booking.getId();
        dto.recurringSeriesId = booking.getRecurringSeriesId();

        dto.userId = booking.getUser().getId();
        dto.userName = booking.getUser().getFullName();

        dto.userRole = booking.getUser().getRole() == null
                ? null
                : booking.getUser().getRole().toString();

        dto.equipmentId = booking.getEquipment().getId();
        dto.equipmentName = booking.getEquipment().getName();
        dto.assetTag = booking.getEquipment().getAssetTag();

        dto.startTime = convertUtcToPlatformTime(
                booking.getStartTime());

        dto.endTime = convertUtcToPlatformTime(
                booking.getEndTime());

        dto.purpose = booking.getPurpose();
        dto.status = booking.getStatus();
        dto.usageMode = booking.getUsageMode();

        dto.createdAt = convertUtcToPlatformTime(
                booking.getCreatedAt());

        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getRecurringSeriesId() {
        return recurringSeriesId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserRole() {
        return userRole;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public UsageMode getUsageMode() {
        return usageMode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
