
package com.labresource.platform.service;

import com.labresource.platform.dto.LiveEquipmentStatusDTO;
import com.labresource.platform.entity.Booking;
import com.labresource.platform.entity.BookingStatus;
import com.labresource.platform.entity.Equipment;
import com.labresource.platform.entity.UsageLog;
import com.labresource.platform.repository.BookingRepository;
import com.labresource.platform.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LiveEquipmentStatusService {

    private final BookingRepository bookingRepository;
    private final UsageLogService usageLogService;
    private final EquipmentRepository equipmentRepository;

    public LiveEquipmentStatusService(
            BookingRepository bookingRepository,
            UsageLogService usageLogService,
            EquipmentRepository equipmentRepository) {

        this.bookingRepository = bookingRepository;
        this.usageLogService = usageLogService;
        this.equipmentRepository = equipmentRepository;
    }

    public List<LiveEquipmentStatusDTO> getLiveEquipmentStatus(
            String status, int page, int size) {

        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException(
                    "Page must be 0 or greater and size must be greater than 0"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        List<Equipment> allEquipment = equipmentRepository.findAll();

        List<Booking> activeBookings =
                bookingRepository.findBookingsByStatusWithDetails(
                        BookingStatus.IN_USE
                );

        List<LiveEquipmentStatusDTO> result = new ArrayList<>();

        for (Equipment equipment : allEquipment) {

            LiveEquipmentStatusDTO dto = new LiveEquipmentStatusDTO();

            dto.setEquipmentId(equipment.getId());
            dto.setEquipmentName(equipment.getName());
            dto.setAssetTag(equipment.getAssetTag());
            dto.setCurrentStatus(equipment.getStatus().name());
            dto.setInUse(false);

            // Check whether this equipment is currently in use
            Booking activeBooking = activeBookings.stream()
                    .filter(booking ->
                            booking.getEquipment().getId()
                                    .equals(equipment.getId()))
                    .findFirst()
                    .orElse(null);

            if (activeBooking != null) {

                dto.setInUse(true);

                UsageLog usageLog =
                        usageLogService.getOpenUsageLog(activeBooking.getId());

                LocalDateTime actualStart =
                        usageLog.getActualStartTime();

                dto.setActualStartTime(actualStart);
                dto.setScheduledEndTime(activeBooking.getEndTime());

                long elapsedMinutes =
                        Duration.between(actualStart, now).toMinutes();

                dto.setElapsedMinutes(Math.max(0, elapsedMinutes));
            }

            // Find the next approved booking
            List<Booking> upcomingBookings =
                    bookingRepository.findUpcomingBookings(
                            equipment.getId(),
                            BookingStatus.APPROVED,
                            now
                    );

            if (!upcomingBookings.isEmpty()) {
                dto.setNextBookingTime(
                        upcomingBookings.get(0).getStartTime()
                );
            }

            result.add(dto);
        }

        // Filter equipment by status
        List<LiveEquipmentStatusDTO> filteredResult = result;

        if (status != null && !status.isBlank()) {
            filteredResult = result.stream()
                    .filter(dto -> dto.getCurrentStatus()
                            .equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        // Apply pagination
        int start = page * size;

        if (start >= filteredResult.size()) {
            return new ArrayList<>();
        }

        int end = Math.min(start + size, filteredResult.size());

        return filteredResult.subList(start, end);
    }
}
