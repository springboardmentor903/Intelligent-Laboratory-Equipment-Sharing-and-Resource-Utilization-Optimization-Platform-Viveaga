
package com.labresource.platform.service;

import com.labresource.platform.dto.*;
import com.labresource.platform.entity.Equipment;
import com.labresource.platform.entity.UsageLog;
import com.labresource.platform.repository.EquipmentRepository;
import com.labresource.platform.repository.UsageLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.labresource.platform.entity.UsageMode;
import com.labresource.platform.dto.EquipmentHeatmapDTO;
import java.util.TreeMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.HashMap;

import com.labresource.platform.entity.BookingStatus;
import com.labresource.platform.repository.BookingRepository;
import java.util.Comparator;

@Service
public class UtilizationService {

    private final EquipmentRepository equipmentRepository;
    private final UsageLogRepository usageLogRepository;
    private final BookingRepository bookingRepository;

    public UtilizationService(
            EquipmentRepository equipmentRepository,
            UsageLogRepository usageLogRepository,
            BookingRepository bookingRepository) {

        this.equipmentRepository = equipmentRepository;
        this.usageLogRepository = usageLogRepository;
        this.bookingRepository = bookingRepository;
    }

    // Equipment-wise utilization calculation
    @Transactional(readOnly = true)
    public List<EquipmentUtilizationDTO> calculateEquipmentUtilization(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Equipment> equipmentList = equipmentRepository.findAll();

        List<UsageLog> usageLogs =
                usageLogRepository.findByActualStartTimeBetween(
                        startDateTime,
                        endDateTime.minusNanos(1)
                );

        List<EquipmentUtilizationDTO> results = new ArrayList<>();

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        for (Equipment equipment : equipmentList) {

            BigDecimal actualUsageMinutes =
                    calculateUsageMinutes(
                            equipment,
                            usageLogs,
                            startDateTime,
                            endDateTime
                    );

            BigDecimal actualUsageHours =
                    actualUsageMinutes.divide(
                            BigDecimal.valueOf(60),
                            2,
                            RoundingMode.HALF_UP
                    );

            BigDecimal availableHours =
                    equipment.getAvailableHoursPerDay()
                            .multiply(BigDecimal.valueOf(days));

            BigDecimal idleHours =
                    availableHours.subtract(actualUsageHours)
                            .max(BigDecimal.ZERO);

            BigDecimal utilizationPercentage =
                    calculatePercentage(
                            actualUsageHours,
                            availableHours
                    );

            results.add(new EquipmentUtilizationDTO(
                    equipment.getId(),
                    equipment.getName(),
                    equipment.getAssetTag(),
                    actualUsageHours,
                    availableHours,
                    idleHours,
                    utilizationPercentage
            ));
        }

        return results;
    }

    // Department-wise utilization calculation
    @Transactional(readOnly = true)
    public List<DepartmentUtilizationDTO> calculateDepartmentUtilization(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        List<Equipment> equipmentList = equipmentRepository.findAll();

        List<UsageLog> usageLogs =
                usageLogRepository.findByActualStartTimeBetween(
                        startDateTime,
                        endDateTime.minusNanos(1)
                );

        Map<Long, DepartmentAggregate> departmentMap =
                new LinkedHashMap<>();

        for (Equipment equipment : equipmentList) {

            if (equipment.getDepartment() == null) {
                continue;
            }

            Long departmentId = equipment.getDepartment().getId();
            String departmentName = equipment.getDepartment().getName();

            BigDecimal actualUsageMinutes =
                    calculateUsageMinutes(
                            equipment,
                            usageLogs,
                            startDateTime,
                            endDateTime
                    );

            BigDecimal actualUsageHours =
                    actualUsageMinutes.divide(
                            BigDecimal.valueOf(60),
                            2,
                            RoundingMode.HALF_UP
                    );

            BigDecimal availableHours =
                    equipment.getAvailableHoursPerDay()
                            .multiply(BigDecimal.valueOf(days));

            DepartmentAggregate aggregate =
                    departmentMap.computeIfAbsent(
                            departmentId,
                            id -> new DepartmentAggregate(
                                    departmentId,
                                    departmentName
                            )
                    );

            aggregate.actualUsageHours =
                    aggregate.actualUsageHours.add(actualUsageHours);

            aggregate.availableHours =
                    aggregate.availableHours.add(availableHours);
        }

        List<DepartmentUtilizationDTO> results = new ArrayList<>();

        for (DepartmentAggregate aggregate : departmentMap.values()) {

            BigDecimal percentage =
                    calculatePercentage(
                            aggregate.actualUsageHours,
                            aggregate.availableHours
                    );

            results.add(new DepartmentUtilizationDTO(
                    aggregate.departmentId,
                    aggregate.departmentName,
                    aggregate.actualUsageHours,
                    aggregate.availableHours,
                    percentage
            ));
        }

        return results;
    }

    // Institution-wise utilization calculation
    @Transactional(readOnly = true)
    public List<InstitutionUtilizationDTO> calculateInstitutionUtilization(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        List<Equipment> equipmentList = equipmentRepository.findAll();

        List<UsageLog> usageLogs =
                usageLogRepository.findByActualStartTimeBetween(
                        startDateTime,
                        endDateTime.minusNanos(1)
                );

        Map<Long, InstitutionAggregate> institutionMap =
                new LinkedHashMap<>();

        for (Equipment equipment : equipmentList) {

            if (equipment.getDepartment() == null
                    || equipment.getDepartment().getInstitution() == null) {
                continue;
            }

            Long institutionId =
                    equipment.getDepartment()
                            .getInstitution().getId();

            String institutionName =
                    equipment.getDepartment()
                            .getInstitution().getName();

            BigDecimal actualUsageMinutes =
                    calculateUsageMinutes(
                            equipment,
                            usageLogs,
                            startDateTime,
                            endDateTime
                    );

            BigDecimal actualUsageHours =
                    actualUsageMinutes.divide(
                            BigDecimal.valueOf(60),
                            2,
                            RoundingMode.HALF_UP
                    );

            BigDecimal availableHours =
                    equipment.getAvailableHoursPerDay()
                            .multiply(BigDecimal.valueOf(days));

            InstitutionAggregate aggregate =
                    institutionMap.computeIfAbsent(
                            institutionId,
                            id -> new InstitutionAggregate(
                                    institutionId,
                                    institutionName
                            )
                    );

            aggregate.actualUsageHours =
                    aggregate.actualUsageHours.add(actualUsageHours);

            aggregate.availableHours =
                    aggregate.availableHours.add(availableHours);
        }

        List<InstitutionUtilizationDTO> results = new ArrayList<>();

        for (InstitutionAggregate aggregate : institutionMap.values()) {

            BigDecimal percentage =
                    calculatePercentage(
                            aggregate.actualUsageHours,
                            aggregate.availableHours
                    );

            results.add(new InstitutionUtilizationDTO(
                    aggregate.institutionId,
                    aggregate.institutionName,
                    aggregate.actualUsageHours,
                    aggregate.availableHours,
                    percentage
            ));
        }

        return results;
    }



    @Transactional(readOnly = true)
    public List<UsageModeUtilizationDTO> calculateUsageModeUtilization(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Equipment> equipmentList = equipmentRepository.findAll();

        List<UsageLog> usageLogs =
                usageLogRepository.findByActualStartTimeBetween(
                        startDateTime,
                        endDateTime.minusNanos(1)
                );

        List<UsageModeUtilizationDTO> results = new ArrayList<>();

        for (Equipment equipment : equipmentList) {

            BigDecimal sharedMinutes = BigDecimal.ZERO;
            BigDecimal exclusiveMinutes = BigDecimal.ZERO;

            for (UsageLog log : usageLogs) {

                if (!log.getEquipment().getId().equals(equipment.getId())
                        || log.getActualEndTime() == null
                        || log.getUsageMode() == null) {
                    continue;
                }

                LocalDateTime effectiveStart =
                        log.getActualStartTime().isBefore(startDateTime)
                                ? startDateTime : log.getActualStartTime();

                LocalDateTime effectiveEnd =
                        log.getActualEndTime().isAfter(endDateTime)
                                ? endDateTime : log.getActualEndTime();

                if (!effectiveEnd.isAfter(effectiveStart)) {
                    continue;
                }

                BigDecimal minutes = BigDecimal.valueOf(
                        ChronoUnit.MINUTES.between(
                                effectiveStart,
                                effectiveEnd
                        )
                );

                if (log.getUsageMode() == UsageMode.SHARED) {
                    sharedMinutes = sharedMinutes.add(minutes);
                } else if (log.getUsageMode() == UsageMode.EXCLUSIVE) {
                    exclusiveMinutes = exclusiveMinutes.add(minutes);
                }
            }

            BigDecimal sharedHours = sharedMinutes.divide(
                    BigDecimal.valueOf(60),
                    2,
                    RoundingMode.HALF_UP
            );

            BigDecimal exclusiveHours = exclusiveMinutes.divide(
                    BigDecimal.valueOf(60),
                    2,
                    RoundingMode.HALF_UP
            );

            BigDecimal totalHours = sharedHours.add(exclusiveHours);

            BigDecimal sharedPercentage =
                    calculatePercentage(sharedHours, totalHours);

            BigDecimal exclusivePercentage =
                    calculatePercentage(exclusiveHours, totalHours);

            results.add(new UsageModeUtilizationDTO(
                    equipment.getId(),
                    equipment.getName(),
                    sharedHours,
                    exclusiveHours,
                    sharedPercentage,
                    exclusivePercentage
            ));
        }

        return results;
    }

    // Equipment-wise demand analysis
    @Transactional(readOnly = true)
    public List<DemandAnalysisDTO> calculateDemandAnalysis(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Equipment> equipmentList = equipmentRepository.findAll();

        List<Object[]> bookingCounts =
                bookingRepository.countBookingsByEquipment(
                        startDateTime,
                        endDateTime,
                        List.of(
                                BookingStatus.PENDING,
                                BookingStatus.APPROVED,
                                BookingStatus.IN_USE,
                                BookingStatus.COMPLETED
                        )
                );

        Map<Long, Long> bookingCountMap = new LinkedHashMap<>();

        for (Object[] row : bookingCounts) {
            Long equipmentId = ((Number) row[0]).longValue();
            Long totalBookings = ((Number) row[1]).longValue();

            bookingCountMap.put(equipmentId, totalBookings);
        }

        List<DemandAnalysisDTO> results = new ArrayList<>();

        for (Equipment equipment : equipmentList) {

            Long totalBookings = bookingCountMap.getOrDefault(
                    equipment.getId(), 0L
            );

            String demandLevel;

            if (totalBookings >= 10) {
                demandLevel = "HIGH";
            } else if (totalBookings >= 5) {
                demandLevel = "MEDIUM";
            } else {
                demandLevel = "LOW";
            }

            results.add(new DemandAnalysisDTO(
                    equipment.getId(),
                    equipment.getName(),
                    totalBookings,
                    demandLevel
            ));
        }

        results.sort(
                Comparator.comparing(
                        DemandAnalysisDTO::getTotalBookings
                ).reversed()
        );

        return results;
    }


    // Historical equipment utilization benchmark
    @Transactional(readOnly = true)
    public List<HistoricalBenchmarkDTO> calculateHistoricalBenchmark(
            LocalDate currentStartDate,
            LocalDate currentEndDate) {

        validateDates(currentStartDate, currentEndDate);

        long periodDays = ChronoUnit.DAYS.between(
                currentStartDate, currentEndDate
        ) + 1;

        LocalDate previousEndDate = currentStartDate.minusDays(1);
        LocalDate previousStartDate = previousEndDate.minusDays(
                periodDays - 1
        );

        List<EquipmentUtilizationDTO> currentResults =
                calculateEquipmentUtilization(
                        currentStartDate, currentEndDate
                );

        List<EquipmentUtilizationDTO> previousResults =
                calculateEquipmentUtilization(
                        previousStartDate, previousEndDate
                );

        Map<Long, EquipmentUtilizationDTO> previousMap =
                new LinkedHashMap<>();

        for (EquipmentUtilizationDTO previous : previousResults) {
            previousMap.put(previous.getEquipmentId(), previous);
        }

        List<HistoricalBenchmarkDTO> results = new ArrayList<>();

        for (EquipmentUtilizationDTO current : currentResults) {

            EquipmentUtilizationDTO previous =
                    previousMap.get(current.getEquipmentId());

            BigDecimal currentPercentage =
                    current.getUtilizationPercentage();

            BigDecimal previousPercentage =
                    previous != null
                            ? previous.getUtilizationPercentage()
                            : BigDecimal.ZERO;

            BigDecimal change = currentPercentage.subtract(
                    previousPercentage
            );

            String trend;

            if (change.compareTo(BigDecimal.ZERO) > 0) {
                trend = "INCREASED";
            } else if (change.compareTo(BigDecimal.ZERO) < 0) {
                trend = "DECREASED";
            } else {
                trend = "UNCHANGED";
            }

            results.add(new HistoricalBenchmarkDTO(
                    current.getEquipmentId(),
                    current.getEquipmentName(),
                    currentPercentage,
                    previousPercentage,
                    change,
                    trend
            ));
        }

        return results;
    }

    // Equipment idle-time detection
    @Transactional(readOnly = true)
    public List<IdleTimeDTO> calculateIdleTime() {

        List<Equipment> equipmentList = equipmentRepository.findAll();

        List<UsageLog> completedLogs =
                usageLogRepository.findByActualEndTimeIsNotNullOrderByActualEndTimeDesc();

        Map<Long, LocalDateTime> lastUsageMap = new LinkedHashMap<>();

        // Store only the latest completed usage time for each equipment
        for (UsageLog log : completedLogs) {

            Long equipmentId = log.getEquipment().getId();

            lastUsageMap.putIfAbsent(
                    equipmentId,
                    log.getActualEndTime()
            );
        }

        LocalDateTime now = LocalDateTime.now();

        List<IdleTimeDTO> results = new ArrayList<>();

        for (Equipment equipment : equipmentList) {

            LocalDateTime lastUsedAt = lastUsageMap.get(equipment.getId());

            Double idleHours = null;
            String idleStatus;

            if (lastUsedAt == null) {

                idleStatus = "NEVER_USED";

            } else {

                long idleMinutes = Math.max(
                        0,
                        ChronoUnit.MINUTES.between(lastUsedAt, now)
                );

                idleHours = Math.round(
                        (idleMinutes / 60.0) * 100.0
                ) / 100.0;

                if (idleHours < 24) {
                    idleStatus = "NORMAL";
                } else if (idleHours <= 72) {
                    idleStatus = "IDLE";
                } else {
                    idleStatus = "HIGH_IDLE";
                }
            }

            results.add(new IdleTimeDTO(
                    equipment.getId(),
                    equipment.getName(),
                    lastUsedAt,
                    idleHours,
                    idleStatus
            ));
        }

        return results;
    }


    // Analyze equipment usage by hour
    @Transactional(readOnly = true)
    public List<PeakUsageDTO> calculateHourlyPeakUsage(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<UsageLog> usageLogs =
                usageLogRepository.findByActualStartTimeBetweenAndActualEndTimeIsNotNull(
                        startDateTime,
                        endDateTime.minusNanos(1)
                );

        Map<Integer, Integer> hourlyCounts = new HashMap<>();

        for (int hour = 0; hour < 24; hour++) {
            hourlyCounts.put(hour, 0);
        }

        for (UsageLog log : usageLogs) {
            int hour = log.getActualStartTime().getHour();

            hourlyCounts.put(
                    hour,
                    hourlyCounts.get(hour) + 1
            );
        }

        int maxCount = hourlyCounts.values()
                .stream()
                .max(Integer::compareTo)
                .orElse(0);

        List<PeakUsageDTO> results = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {

            String period = String.format("%02d:00", hour);

            int count = hourlyCounts.get(hour);

            results.add(new PeakUsageDTO(
                    period,
                    count,
                    maxCount > 0 && count == maxCount
            ));
        }

        return results;
    }


    // Analyze equipment usage by day of the week
    @Transactional(readOnly = true)
    public List<PeakUsageDTO> calculateDailyPeakUsage(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<UsageLog> usageLogs =
                usageLogRepository.findByActualStartTimeBetweenAndActualEndTimeIsNotNull(
                        startDateTime,
                        endDateTime.minusNanos(1)
                );

        Map<DayOfWeek, Integer> dailyCounts = new HashMap<>();

        for (DayOfWeek day : DayOfWeek.values()) {
            dailyCounts.put(day, 0);
        }

        for (UsageLog log : usageLogs) {

            DayOfWeek day = log.getActualStartTime().getDayOfWeek();

            dailyCounts.put(
                    day,
                    dailyCounts.get(day) + 1
            );
        }

        int maxCount = dailyCounts.values()
                .stream()
                .max(Integer::compareTo)
                .orElse(0);

        List<PeakUsageDTO> results = new ArrayList<>();

        for (DayOfWeek day : DayOfWeek.values()) {

            String period = day.getDisplayName(
                    TextStyle.FULL,
                    Locale.ENGLISH
            );

            int count = dailyCounts.get(day);

            results.add(new PeakUsageDTO(
                    period,
                    count,
                    maxCount > 0 && count == maxCount
            ));
        }

        return results;
    }

    // Generate equipment-wise hourly usage heatmap
    @Transactional(readOnly = true)
    public List<EquipmentHeatmapDTO> calculateEquipmentHeatmap(
            LocalDate startDate,
            LocalDate endDate
    ) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Equipment> equipmentList = equipmentRepository.findAll();

        List<EquipmentHeatmapDTO> results = new ArrayList<>();

        for (Equipment equipment : equipmentList) {

            List<UsageLog> usageLogs =
                    usageLogRepository
                            .findByEquipmentIdAndActualStartTimeBetweenAndActualEndTimeIsNotNull(
                                    equipment.getId(),
                                    startDateTime,
                                    endDateTime.minusNanos(1)
                            );

            Map<String, Integer> hourlyUsage = new TreeMap<>();

            // Initialize all 24 hours with zero usage
            for (int hour = 0; hour < 24; hour++) {
                String period = String.format("%02d:00", hour);
                hourlyUsage.put(period, 0);
            }

            // Count completed sessions by their actual start hour
            for (UsageLog log : usageLogs) {

                int hour = log.getActualStartTime().getHour();

                String period = String.format("%02d:00", hour);

                hourlyUsage.put(
                        period,
                        hourlyUsage.get(period) + 1
                );
            }

            results.add(
                    new EquipmentHeatmapDTO(
                            equipment.getId(),
                            equipment.getName(),
                            hourlyUsage
                    )
            );
        }

        return results;
    }


    // Calculate actual usage minutes
    private BigDecimal calculateUsageMinutes(
            Equipment equipment,
            List<UsageLog> usageLogs,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime) {

        BigDecimal actualUsageMinutes = BigDecimal.ZERO;

        for (UsageLog log : usageLogs) {

            if (log.getEquipment().getId().equals(equipment.getId())
                    && log.getActualEndTime() != null) {

                LocalDateTime usageStart = log.getActualStartTime();
                LocalDateTime usageEnd = log.getActualEndTime();

                LocalDateTime effectiveStart =
                        usageStart.isBefore(startDateTime)
                                ? startDateTime : usageStart;

                LocalDateTime effectiveEnd =
                        usageEnd.isAfter(endDateTime)
                                ? endDateTime : usageEnd;

                if (effectiveEnd.isAfter(effectiveStart)) {

                    long minutes = ChronoUnit.MINUTES.between(
                            effectiveStart,
                            effectiveEnd
                    );

                    actualUsageMinutes =
                            actualUsageMinutes.add(
                                    BigDecimal.valueOf(minutes)
                            );
                }
            }
        }

        return actualUsageMinutes;
    }

    // Calculate utilization percentage
    private BigDecimal calculatePercentage(
            BigDecimal actualUsageHours,
            BigDecimal availableHours) {

        if (availableHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return actualUsageHours
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        availableHours,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    // Validate date range
    private void validateDates(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null || endDate == null
                || endDate.isBefore(startDate)) {

            throw new IllegalArgumentException(
                    "Valid start date and end date are required"
            );
        }
    }

    // Department aggregation helper
    private static class DepartmentAggregate {

        private final Long departmentId;
        private final String departmentName;

        private BigDecimal actualUsageHours = BigDecimal.ZERO;
        private BigDecimal availableHours = BigDecimal.ZERO;

        private DepartmentAggregate(
                Long departmentId,
                String departmentName) {

            this.departmentId = departmentId;
            this.departmentName = departmentName;
        }
    }

    // Institution aggregation helper
    private static class InstitutionAggregate {

        private final Long institutionId;
        private final String institutionName;

        private BigDecimal actualUsageHours = BigDecimal.ZERO;
        private BigDecimal availableHours = BigDecimal.ZERO;

        private InstitutionAggregate(
                Long institutionId,
                String institutionName) {

            this.institutionId = institutionId;
            this.institutionName = institutionName;
        }
    }
}
