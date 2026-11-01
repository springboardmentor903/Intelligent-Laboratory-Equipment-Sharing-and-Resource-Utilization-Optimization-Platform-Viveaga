
package com.labresource.platform.dto;

import com.labresource.platform.entity.Booking;
import com.labresource.platform.entity.BookingStatus;
import com.labresource.platform.entity.UsageMode;

import java.time.LocalDateTime;

public class BookingResponseDTO {

    private Long id;
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

    public static BookingResponseDTO fromEntity(Booking booking) {
        BookingResponseDTO dto = new BookingResponseDTO();

        dto.id = booking.getId();
        dto.userId = booking.getUser().getId();
        dto.userName = booking.getUser().getFullName();

        dto.userRole = booking.getUser().getRole() == null
                ? null
                : booking.getUser().getRole().toString();

        dto.equipmentId = booking.getEquipment().getId();
        dto.equipmentName = booking.getEquipment().getName();
        dto.assetTag = booking.getEquipment().getAssetTag();

        dto.startTime = booking.getStartTime();
        dto.endTime = booking.getEndTime();
        dto.purpose = booking.getPurpose();
        dto.status = booking.getStatus();
        dto.usageMode = booking.getUsageMode();
        dto.createdAt = booking.getCreatedAt();

        return dto;
    }

    public Long getId() {
        return id;
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
