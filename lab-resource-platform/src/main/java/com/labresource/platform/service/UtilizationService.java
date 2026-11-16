package com.labresource.platform.service;

import com.labresource.platform.dto.*;
import com.labresource.platform.entity.*;
import com.labresource.platform.repository.EquipmentRepository;
import com.labresource.platform.repository.UsageLogRepository;
import com.labresource.platform.repository.BookingRepository;
import com.labresource.platform.repository.EquipmentOperatingHoursRepository;
import com.labresource.platform.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UtilizationService {

    private final EquipmentRepository equipmentRepository;
    private final UsageLogRepository usageLogRepository;
    private final BookingRepository bookingRepository;
    private final EquipmentOperatingHoursRepository operatingHoursRepository;
    private final ZoneId platformTimeZone;
    private final UserRepository userRepository;

    public UtilizationService(
            EquipmentRepository equipmentRepository,
            UsageLogRepository usageLogRepository,
            BookingRepository bookingRepository,
            EquipmentOperatingHoursRepository operatingHoursRepository,
            ZoneId platformTimeZone,
            UserRepository userRepository) {

        this.equipmentRepository = equipmentRepository;
        this.usageLogRepository = usageLogRepository;
        this.bookingRepository = bookingRepository;
        this.operatingHoursRepository = operatingHoursRepository;
        this.platformTimeZone = platformTimeZone;
        this.userRepository = userRepository;
    }

    // ==================================================
    // ROLE-BASED EQUIPMENT ACCESS
    // ==================================================

    /**
     * Returns only the equipment the logged-in user is permitted to analyse.
     *
     * SYSTEM_ADMINISTRATOR:
     *     All institutions.
     *
     * INSTITUTION_ADMINISTRATOR:
     *     All departments in their institution.
     *
     * LAB_MANAGER / DEPARTMENT_HEAD:
     *     Their assigned department only.
     *
     * LAB_TECHNICIAN / RESEARCHER_STUDENT:
     *     No access to detailed utilization analytics.
     */

    private List<Equipment> getAccessibleEquipment() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().equals("anonymousUser")) {

            throw new AccessDeniedException(
                    "Authentication is required to access utilization analytics"
            );
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException(
                        "Logged-in user account was not found"
                ));

        Role role = user.getRole();

        if (role == null) {
            throw new AccessDeniedException(
                    "Your account does not have an assigned role"
            );
        }

        // System Administrator: all institutions
        if (role == Role.SYSTEM_ADMINISTRATOR) {
            return equipmentRepository.findAll();
        }

        // Institution Administrator: entire own institution
        // Lab Manager: entire own institution
        if (role == Role.INSTITUTION_ADMINISTRATOR
                || role == Role.LAB_MANAGER) {

            if (user.getInstitution() == null) {
                throw new AccessDeniedException(
                        "Your account is not assigned to an institution"
                );
            }

            Long institutionId = user.getInstitution().getId();

            return equipmentRepository.findAll()
                    .stream()
                    .filter(equipment ->
                            equipment.getDepartment() != null
                                    && equipment.getDepartment().getInstitution() != null
                                    && Objects.equals(
                                    equipment.getDepartment()
                                            .getInstitution().getId(),
                                    institutionId
                            )
                    )
                    .toList();
        }

        // Department Head: own department only
        if (role == Role.DEPARTMENT_HEAD) {

            if (user.getDepartment() == null) {
                throw new AccessDeniedException(
                        "Your account is not assigned to a department"
                );
            }

            Long departmentId = user.getDepartment().getId();

            return equipmentRepository.findAll()
                    .stream()
                    .filter(equipment ->
                            equipment.getDepartment() != null
                                    && Objects.equals(
                                    equipment.getDepartment().getId(),
                                    departmentId
                            )
                    )
                    .toList();
        }

        // Technicians and students cannot access detailed analytics
        throw new AccessDeniedException(
                "You do not have permission to access detailed utilization analytics"
        );
    }

    /**
     * Restricts usage logs to the permitted equipment IDs.
     */
    private List<UsageLog> filterUsageLogs(
            List<UsageLog> usageLogs,
            List<Equipment> equipmentList) {

        Set<Long> allowedEquipmentIds = equipmentList.stream()
                .map(Equipment::getId)
                .collect(Collectors.toSet());

        return usageLogs.stream()
                .filter(log ->
                        log.getEquipment() != null
                                && log.getEquipment().getId() != null
                                && allowedEquipmentIds.contains(
                                log.getEquipment().getId()
                        )
                )
                .toList();
    }

    // ==================================================
    // EQUIPMENT UTILIZATION
    // ==================================================


    @Transactional(readOnly = true)
    public List<EquipmentUtilizationDTO> calculateEquipmentUtilization(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        LocalDateTime utcStartDateTime =
                convertPlatformToUtc(startDateTime);

        LocalDateTime utcEndDateTime =
                convertPlatformToUtc(endDateTime);

        List<Equipment> equipmentList = getAccessibleEquipment();

        if (equipmentList.isEmpty()) {
            return List.of();
        }

        List<UsageLog> usageLogs = filterUsageLogs(
                usageLogRepository.findOverlappingUsageLogs(
                        utcStartDateTime,
                        utcEndDateTime
                ),
                equipmentList
        );

        Map<Long, List<EquipmentOperatingHours>> scheduleMap =
                loadOperatingSchedules(equipmentList);

        List<EquipmentUtilizationDTO> results = new ArrayList<>();

        LocalDateTime now = convertUtcToPlatform(
                LocalDateTime.now(ZoneOffset.UTC)
        );

        // Bookings that reserve equipment capacity
        List<BookingStatus> bookedStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.APPROVED,
                BookingStatus.IN_USE,
                BookingStatus.COMPLETED
        );

        for (Equipment equipment : equipmentList) {

            Long equipmentId = equipment.getId();

            List<EquipmentOperatingHours> schedule =
                    scheduleMap.getOrDefault(equipmentId, List.of());

            // 1. Available operating minutes
            BigDecimal availableMinutes =
                    calculateAvailableMinutes(
                            startDate,
                            endDate,
                            schedule
                    );

            // 2. Actual usage minutes
            BigDecimal actualUsageMinutes =
                    calculateUsageMinutes(
                            equipmentId,
                            usageLogs,
                            startDateTime,
                            endDateTime,
                            schedule,
                            now
                    );

            // 3. Booked minutes
            List<Booking> bookings =
                    bookingRepository.findOverlappingBookings(
                            equipmentId,
                            utcStartDateTime,
                            utcEndDateTime,
                            bookedStatuses
                    );

            BigDecimal bookedMinutes = BigDecimal.ZERO;

            for (Booking booking : bookings) {

                LocalDateTime bookingStart =
                        convertUtcToPlatform(booking.getStartTime());

                LocalDateTime bookingEnd =
                        convertUtcToPlatform(booking.getEndTime());

                // Count only elapsed booking time
                if (!bookingStart.isBefore(now)) {
                    continue;
                }

                if (bookingEnd.isAfter(now)) {
                    bookingEnd = now;
                }

                BigDecimal bookingDuration =
                        calculateSingleSessionMinutes(
                                bookingStart,
                                bookingEnd,
                                startDateTime,
                                endDateTime,
                                schedule
                        );

                bookedMinutes = bookedMinutes.add(bookingDuration);
            }

            // Convert minutes into hours
            BigDecimal actualUsageHours = toHours(actualUsageMinutes);
            BigDecimal bookedHours = toHours(bookedMinutes);
            BigDecimal availableHours = toHours(availableMinutes);

            // 4. Idle = available - booked
            BigDecimal idleHours = availableHours
                    .subtract(bookedHours)
                    .max(BigDecimal.ZERO);

            // 5. Utilization = actual usage / available capacity
            BigDecimal utilizationPercentage =
                    calculatePercentage(
                            actualUsageHours,
                            availableHours
                    );

            results.add(new EquipmentUtilizationDTO(
                    equipmentId,
                    equipment.getName(),
                    equipment.getAssetTag(),
                    actualUsageHours,
                    bookedHours,
                    availableHours,
                    idleHours,
                    utilizationPercentage
            ));
        }

        return results;
    }


    // ==================================================
    // DEPARTMENT UTILIZATION
    // ==================================================

    @Transactional(readOnly = true)
    public List<DepartmentUtilizationDTO> calculateDepartmentUtilization(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        LocalDateTime utcStartDateTime =
                convertPlatformToUtc(startDateTime);

        LocalDateTime utcEndDateTime =
                convertPlatformToUtc(endDateTime);

        List<Equipment> equipmentList = getAccessibleEquipment();

        if (equipmentList.isEmpty()) {
            return List.of();
        }

        List<UsageLog> usageLogs = filterUsageLogs(
                usageLogRepository.findOverlappingUsageLogs(
                        utcStartDateTime,
                        utcEndDateTime
                ),
                equipmentList
        );

        Map<Long, List<EquipmentOperatingHours>> scheduleMap =
                loadOperatingSchedules(equipmentList);

        Map<Long, DepartmentAggregate> departmentMap =
                new LinkedHashMap<>();

        LocalDateTime now = convertUtcToPlatform(
                LocalDateTime.now(ZoneOffset.UTC)
        );

        for (Equipment equipment : equipmentList) {

            if (equipment.getDepartment() == null) {
                continue;
            }

            Long departmentId = equipment.getDepartment().getId();
            String departmentName = equipment.getDepartment().getName();

            List<EquipmentOperatingHours> schedule =
                    scheduleMap.getOrDefault(
                            equipment.getId(),
                            List.of()
                    );

            BigDecimal usedMinutes = calculateUsageMinutes(
                    equipment.getId(),
                    usageLogs,
                    startDateTime,
                    endDateTime,
                    schedule,
                    now
            );

            BigDecimal availableMinutes =
                    calculateAvailableMinutes(
                            startDate,
                            endDate,
                            schedule
                    );

            DepartmentAggregate aggregate =
                    departmentMap.computeIfAbsent(
                            departmentId,
                            id -> new DepartmentAggregate(
                                    departmentId,
                                    departmentName
                            )
                    );

            aggregate.actualUsageMinutes =
                    aggregate.actualUsageMinutes.add(usedMinutes);

            aggregate.availableMinutes =
                    aggregate.availableMinutes.add(availableMinutes);
        }

        List<DepartmentUtilizationDTO> results = new ArrayList<>();

        for (DepartmentAggregate aggregate : departmentMap.values()) {

            BigDecimal usedHours =
                    toHours(aggregate.actualUsageMinutes);

            BigDecimal availableHours =
                    toHours(aggregate.availableMinutes);

            BigDecimal percentage =
                    calculatePercentage(usedHours, availableHours);

            results.add(new DepartmentUtilizationDTO(
                    aggregate.departmentId,
                    aggregate.departmentName,
                    usedHours,
                    availableHours,
                    percentage
            ));
        }

        return results;
    }

    // ==================================================
    // INSTITUTION UTILIZATION
    // ==================================================

    @Transactional(readOnly = true)
    public List<InstitutionUtilizationDTO> calculateInstitutionUtilization(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        LocalDateTime utcStartDateTime =
                convertPlatformToUtc(startDateTime);

        LocalDateTime utcEndDateTime =
                convertPlatformToUtc(endDateTime);

        List<Equipment> equipmentList = getAccessibleEquipment();

        if (equipmentList.isEmpty()) {
            return List.of();
        }

        List<UsageLog> usageLogs = filterUsageLogs(
                usageLogRepository.findOverlappingUsageLogs(
                        utcStartDateTime,
                        utcEndDateTime
                ),
                equipmentList
        );

        Map<Long, List<EquipmentOperatingHours>> scheduleMap =
                loadOperatingSchedules(equipmentList);

        Map<Long, InstitutionAggregate> institutionMap =
                new LinkedHashMap<>();

        LocalDateTime now = convertUtcToPlatform(
                LocalDateTime.now(ZoneOffset.UTC)
        );

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

            List<EquipmentOperatingHours> schedule =
                    scheduleMap.getOrDefault(
                            equipment.getId(),
                            List.of()
                    );

            BigDecimal usedMinutes = calculateUsageMinutes(
                    equipment.getId(),
                    usageLogs,
                    startDateTime,
                    endDateTime,
                    schedule,
                    now
            );

            BigDecimal availableMinutes =
                    calculateAvailableMinutes(
                            startDate,
                            endDate,
                            schedule
                    );

            InstitutionAggregate aggregate =
                    institutionMap.computeIfAbsent(
                            institutionId,
                            id -> new InstitutionAggregate(
                                    institutionId,
                                    institutionName
                            )
                    );

            aggregate.actualUsageMinutes =
                    aggregate.actualUsageMinutes.add(usedMinutes);

            aggregate.availableMinutes =
                    aggregate.availableMinutes.add(availableMinutes);
        }

        List<InstitutionUtilizationDTO> results = new ArrayList<>();

        for (InstitutionAggregate aggregate : institutionMap.values()) {

            BigDecimal usedHours =
                    toHours(aggregate.actualUsageMinutes);

            BigDecimal availableHours =
                    toHours(aggregate.availableMinutes);

            BigDecimal percentage =
                    calculatePercentage(usedHours, availableHours);

            results.add(new InstitutionUtilizationDTO(
                    aggregate.institutionId,
                    aggregate.institutionName,
                    usedHours,
                    availableHours,
                    percentage
            ));
        }

        return results;
    }

    // ==================================================
    // USAGE MODE UTILIZATION
    // ==================================================

    @Transactional(readOnly = true)
    public List<UsageModeUtilizationDTO> calculateUsageModeUtilization(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        LocalDateTime utcStartDateTime =
                convertPlatformToUtc(startDateTime);

        LocalDateTime utcEndDateTime =
                convertPlatformToUtc(endDateTime);

        List<Equipment> equipmentList = getAccessibleEquipment();

        if (equipmentList.isEmpty()) {
            return List.of();
        }

        List<UsageLog> usageLogs = filterUsageLogs(
                usageLogRepository.findOverlappingUsageLogs(
                        utcStartDateTime,
                        utcEndDateTime
                ),
                equipmentList
        );

        Map<Long, List<EquipmentOperatingHours>> scheduleMap =
                loadOperatingSchedules(equipmentList);

        LocalDateTime now = convertUtcToPlatform(
                LocalDateTime.now(ZoneOffset.UTC)
        );

        List<UsageModeUtilizationDTO> results = new ArrayList<>();

        for (Equipment equipment : equipmentList) {

            List<EquipmentOperatingHours> schedule =
                    scheduleMap.getOrDefault(
                            equipment.getId(),
                            List.of()
                    );

            BigDecimal sharedMinutes = BigDecimal.ZERO;
            BigDecimal exclusiveMinutes = BigDecimal.ZERO;

            for (UsageLog log : usageLogs) {

                if (!log.getEquipment().getId().equals(equipment.getId())
                        || log.getUsageMode() == null
                        || log.getActualStartTime() == null) {
                    continue;
                }

                LocalDateTime usageStart =
                        convertUtcToPlatform(log.getActualStartTime());

                LocalDateTime usageEnd =
                        log.getActualEndTime() == null
                                ? now
                                : convertUtcToPlatform(log.getActualEndTime());

                BigDecimal minutes = calculateSingleSessionMinutes(
                        usageStart,
                        usageEnd,
                        startDateTime,
                        endDateTime,
                        schedule
                );

                if (log.getUsageMode() == UsageMode.SHARED) {
                    sharedMinutes = sharedMinutes.add(minutes);
                } else if (log.getUsageMode() == UsageMode.EXCLUSIVE) {
                    exclusiveMinutes = exclusiveMinutes.add(minutes);
                }
            }

            BigDecimal sharedHours = toHours(sharedMinutes);
            BigDecimal exclusiveHours = toHours(exclusiveMinutes);

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

    // ==================================================
    // DEMAND ANALYSIS
    // ==================================================


    @Transactional(readOnly = true)
    public List<DemandAnalysisDTO> calculateDemandAnalysis(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        // Convert platform-local date boundaries to UTC
        LocalDateTime startDateTime = convertPlatformToUtc(
                startDate.atStartOfDay()
        );

        LocalDateTime endDateTime = convertPlatformToUtc(
                endDate.plusDays(1).atStartOfDay()
        );

        List<Equipment> equipmentList = getAccessibleEquipment();

        if (equipmentList.isEmpty()) {
            return List.of();
        }

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

        Set<Long> allowedEquipmentIds = equipmentList.stream()
                .map(Equipment::getId)
                .collect(Collectors.toSet());

        for (Object[] row : bookingCounts) {

            Long equipmentId = ((Number) row[0]).longValue();

            if (!allowedEquipmentIds.contains(equipmentId)) {
                continue;
            }

            Long totalBookings = ((Number) row[1]).longValue();

            bookingCountMap.put(equipmentId, totalBookings);
        }

        List<DemandAnalysisDTO> results = new ArrayList<>();

        for (Equipment equipment : equipmentList) {

            Long totalBookings = bookingCountMap.getOrDefault(
                    equipment.getId(),
                    0L
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


    // ==================================================
    // HISTORICAL BENCHMARK
    // ==================================================


    @Transactional(readOnly = true)
    public List<HistoricalBenchmarkDTO> calculateHistoricalBenchmark(
            LocalDate currentStartDate,
            LocalDate currentEndDate) {

        validateDates(currentStartDate, currentEndDate);

        // Previous 28 days, excluding the current period
        LocalDate previousEndDate = currentStartDate.minusDays(1);
        LocalDate previousStartDate = previousEndDate.minusDays(27);

        List<EquipmentUtilizationDTO> currentResults =
                calculateEquipmentUtilization(
                        currentStartDate,
                        currentEndDate
                );

        List<EquipmentUtilizationDTO> previousResults =
                calculateEquipmentUtilization(
                        previousStartDate,
                        previousEndDate
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

            BigDecimal change =
                    currentPercentage.subtract(previousPercentage);

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


    // ==================================================
    // IDLE TIME
    // ==================================================



    @Transactional(readOnly = true)
    public List<IdleTimeDTO> calculateIdleTime() {

        // Last 7 days, including today
        LocalDate today = LocalDate.now(platformTimeZone);
        LocalDate startDate = today.minusDays(6);
        LocalDate endDate = today;

        LocalDateTime localStart = startDate.atStartOfDay();
        LocalDateTime localEnd = endDate.plusDays(1).atStartOfDay();

        LocalDateTime utcStart = convertPlatformToUtc(localStart);
        LocalDateTime utcEnd = convertPlatformToUtc(localEnd);

        // Apply role-based access
        List<Equipment> equipmentList = getAccessibleEquipment()
                .stream()
                .filter(equipment ->
                        equipment.getStatus() == EquipmentStatus.AVAILABLE
                                || equipment.getStatus() == EquipmentStatus.BOOKED
                )
                .toList();

        if (equipmentList.isEmpty()) {
            return List.of();
        }

        List<Long> equipmentIds = equipmentList.stream()
                .map(Equipment::getId)
                .toList();

        // Load operating schedules
        List<EquipmentOperatingHours> allSchedules =
                operatingHoursRepository.findAllByEquipment_IdIn(
                        equipmentIds
                );

        Map<Long, List<EquipmentOperatingHours>> scheduleMap =
                new HashMap<>();

        for (EquipmentOperatingHours hours : allSchedules) {
            Long equipmentId = hours.getEquipment().getId();

            scheduleMap.computeIfAbsent(
                    equipmentId,
                    id -> new ArrayList<>()
            ).add(hours);
        }

        // Load actual usage logs, including active sessions
        List<UsageLog> usageLogs =
                usageLogRepository.findOverlappingUsageLogs(
                        utcStart,
                        utcEnd
                );

        Map<Long, List<UsageLog>> usageMap = new HashMap<>();

        for (UsageLog log : usageLogs) {
            Long equipmentId = log.getEquipment().getId();

            if (!equipmentIds.contains(equipmentId)) {
                continue;
            }

            usageMap.computeIfAbsent(
                    equipmentId,
                    id -> new ArrayList<>()
            ).add(log);
        }

        List<IdleTimeDTO> results = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now(platformTimeZone);

        for (Equipment equipment : equipmentList) {

            Long equipmentId = equipment.getId();

            List<EquipmentOperatingHours> schedule =
                    scheduleMap.getOrDefault(
                            equipmentId,
                            new ArrayList<>()
                    );

            List<UsageLog> equipmentLogs =
                    usageMap.getOrDefault(
                            equipmentId,
                            new ArrayList<>()
                    );

            // Calculate available operating minutes
            // calculateAvailableMinutes() already caps at current time
            BigDecimal availableMinutes = BigDecimal.ZERO;

            for (LocalDate date = startDate;
                 !date.isAfter(endDate);
                 date = date.plusDays(1)) {

                availableMinutes = availableMinutes.add(
                        calculateAvailableMinutes(
                                date,
                                date,
                                schedule
                        )
                );
            }

            if (availableMinutes.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Calculate actual used minutes without double counting
            List<LocalDateTime[]> intervals = new ArrayList<>();

            for (UsageLog log : equipmentLogs) {

                LocalDateTime usageStart =
                        convertUtcToPlatform(log.getActualStartTime());

                LocalDateTime usageEnd =
                        log.getActualEndTime() != null
                                ? convertUtcToPlatform(log.getActualEndTime())
                                : now;

                // Clip intervals to the reporting period
                if (usageStart.isBefore(localStart)) {
                    usageStart = localStart;
                }

                if (usageEnd.isAfter(now)) {
                    usageEnd = now;
                }

                if (usageEnd.isAfter(usageStart)) {
                    intervals.add(new LocalDateTime[]{
                            usageStart,
                            usageEnd
                    });
                }
            }

            // Sort intervals by start time
            intervals.sort(
                    Comparator.comparing(interval -> interval[0])
            );

            BigDecimal usedMinutes = BigDecimal.ZERO;

            LocalDateTime mergedStart = null;
            LocalDateTime mergedEnd = null;

            for (LocalDateTime[] interval : intervals) {

                LocalDateTime intervalStart = interval[0];
                LocalDateTime intervalEnd = interval[1];

                if (mergedStart == null) {
                    mergedStart = intervalStart;
                    mergedEnd = intervalEnd;
                    continue;
                }

                // Merge overlapping or touching intervals
                if (!intervalStart.isAfter(mergedEnd)) {

                    if (intervalEnd.isAfter(mergedEnd)) {
                        mergedEnd = intervalEnd;
                    }

                } else {

                    usedMinutes = usedMinutes.add(
                            calculateSingleSessionMinutes(
                                    mergedStart,
                                    mergedEnd,
                                    localStart,
                                    localEnd,
                                    schedule
                            )
                    );

                    mergedStart = intervalStart;
                    mergedEnd = intervalEnd;
                }
            }

            // Add final merged interval
            if (mergedStart != null) {
                usedMinutes = usedMinutes.add(
                        calculateSingleSessionMinutes(
                                mergedStart,
                                mergedEnd,
                                localStart,
                                localEnd,
                                schedule
                        )
                );
            }

            // Actual idle time = available time - actual used time
            BigDecimal idleMinutes =
                    availableMinutes.subtract(usedMinutes);

            if (idleMinutes.compareTo(BigDecimal.ZERO) < 0) {
                idleMinutes = BigDecimal.ZERO;
            }

            BigDecimal idlePercentage = idleMinutes
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            availableMinutes,
                            2,
                            RoundingMode.HALF_UP
                    );

            // Retain the existing 90% idle alert threshold
            if (idlePercentage.compareTo(BigDecimal.valueOf(90)) >= 0) {

                BigDecimal idleHours = idleMinutes.divide(
                        BigDecimal.valueOf(60),
                        2,
                        RoundingMode.HALF_UP
                );

                String departmentName =
                        equipment.getDepartment() != null
                                ? equipment.getDepartment().getName()
                                : null;

                // Preserve the last booking end time
                LocalDateTime lastBookingEndedAt = null;

                List<BookingStatus> validStatuses = List.of(
                        BookingStatus.PENDING,
                        BookingStatus.APPROVED,
                        BookingStatus.IN_USE,
                        BookingStatus.COMPLETED
                );

                List<Booking> equipmentBookings =
                        bookingRepository.findOverlappingBookings(
                                equipmentId,
                                utcStart,
                                utcEnd,
                                validStatuses
                        );

                for (Booking booking : equipmentBookings) {

                    LocalDateTime bookingEnd =
                            convertUtcToPlatform(booking.getEndTime());

                    if (!bookingEnd.isAfter(now)
                            && (lastBookingEndedAt == null
                            || bookingEnd.isAfter(lastBookingEndedAt))) {

                        lastBookingEndedAt = bookingEnd;
                    }
                }

                results.add(new IdleTimeDTO(
                        equipment.getId(),
                        equipment.getName(),
                        equipment.getAssetTag(),
                        departmentName,
                        idlePercentage.doubleValue(),
                        idleHours.doubleValue(),
                        lastBookingEndedAt
                ));
            }
        }

        return results;
    }



    // ==================================================
    // HOURLY PEAK USAGE
    // ==================================================

    @Transactional(readOnly = true)
    public List<PeakUsageDTO> calculateHourlyPeakUsage(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime localStart = startDate.atStartOfDay();
        LocalDateTime localEnd = endDate.plusDays(1).atStartOfDay();

        LocalDateTime utcStart = convertPlatformToUtc(localStart);
        LocalDateTime utcEnd = convertPlatformToUtc(localEnd);

        List<Equipment> equipmentList = getAccessibleEquipment();

        if (equipmentList.isEmpty()) {
            return List.of();
        }

        List<UsageLog> usageLogs = filterUsageLogs(
                usageLogRepository.findOverlappingUsageLogs(
                        utcStart,
                        utcEnd
                ),
                equipmentList
        );

        LocalDateTime now = convertUtcToPlatform(
                LocalDateTime.now(ZoneOffset.UTC)
        );

        Map<Integer, BigDecimal> hourlyMinutes = new HashMap<>();

        for (int hour = 0; hour < 24; hour++) {
            hourlyMinutes.put(hour, BigDecimal.ZERO);
        }

        for (UsageLog log : usageLogs) {

            if (log.getActualStartTime() == null) {
                continue;
            }

            LocalDateTime usageStart =
                    convertUtcToPlatform(log.getActualStartTime());

            LocalDateTime usageEnd =
                    log.getActualEndTime() == null
                            ? now
                            : convertUtcToPlatform(log.getActualEndTime());

            LocalDateTime effectiveStart =
                    usageStart.isAfter(localStart)
                            ? usageStart
                            : localStart;

            LocalDateTime effectiveEnd =
                    usageEnd.isBefore(localEnd)
                            ? usageEnd
                            : localEnd;

            if (effectiveEnd.isAfter(now)) {
                effectiveEnd = now;
            }

            if (!effectiveEnd.isAfter(effectiveStart)) {
                continue;
            }

            LocalDateTime cursor = effectiveStart;

            while (cursor.isBefore(effectiveEnd)) {

                LocalDateTime nextHour = cursor
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .plusHours(1);

                LocalDateTime segmentEnd =
                        effectiveEnd.isBefore(nextHour)
                                ? effectiveEnd
                                : nextHour;

                long seconds = ChronoUnit.SECONDS.between(
                        cursor,
                        segmentEnd
                );

                BigDecimal minutes = BigDecimal.valueOf(seconds)
                        .divide(
                                BigDecimal.valueOf(60),
                                2,
                                RoundingMode.HALF_UP
                        );

                int hour = cursor.getHour();

                hourlyMinutes.merge(
                        hour,
                        minutes,
                        BigDecimal::add
                );

                cursor = segmentEnd;
            }
        }

        BigDecimal maxMinutes = hourlyMinutes.values()
                .stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<PeakUsageDTO> results = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {

            String period = String.format("%02d:00", hour);

            BigDecimal minutes = hourlyMinutes.get(hour);

            boolean isPeak =
                    maxMinutes.compareTo(BigDecimal.ZERO) > 0
                            && minutes.compareTo(maxMinutes) == 0;

            results.add(new PeakUsageDTO(
                    period,
                    minutes,
                    isPeak
            ));
        }

        return results;
    }

    // ==================================================
    // DAILY PEAK USAGE
    // ==================================================

    @Transactional(readOnly = true)
    public List<PeakUsageDTO> calculateDailyPeakUsage(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime localStart = startDate.atStartOfDay();
        LocalDateTime localEnd = endDate.plusDays(1).atStartOfDay();

        LocalDateTime utcStart = convertPlatformToUtc(localStart);
        LocalDateTime utcEnd = convertPlatformToUtc(localEnd);

        List<Equipment> equipmentList = getAccessibleEquipment();

        if (equipmentList.isEmpty()) {
            return List.of();
        }

        List<UsageLog> usageLogs = filterUsageLogs(
                usageLogRepository.findOverlappingUsageLogs(
                        utcStart,
                        utcEnd
                ),
                equipmentList
        );

        LocalDateTime now = convertUtcToPlatform(
                LocalDateTime.now(ZoneOffset.UTC)
        );

        Map<DayOfWeek, BigDecimal> dailyMinutes = new EnumMap<>(
                DayOfWeek.class
        );

        for (DayOfWeek day : DayOfWeek.values()) {
            dailyMinutes.put(day, BigDecimal.ZERO);
        }

        for (UsageLog log : usageLogs) {

            if (log.getActualStartTime() == null) {
                continue;
            }

            LocalDateTime usageStart =
                    convertUtcToPlatform(log.getActualStartTime());

            LocalDateTime usageEnd =
                    log.getActualEndTime() == null
                            ? now
                            : convertUtcToPlatform(log.getActualEndTime());

            LocalDateTime effectiveStart =
                    usageStart.isAfter(localStart)
                            ? usageStart
                            : localStart;

            LocalDateTime effectiveEnd =
                    usageEnd.isBefore(localEnd)
                            ? usageEnd
                            : localEnd;

            if (effectiveEnd.isAfter(now)) {
                effectiveEnd = now;
            }

            if (!effectiveEnd.isAfter(effectiveStart)) {
                continue;
            }

            LocalDate date = effectiveStart.toLocalDate();

            while (!date.isAfter(effectiveEnd.toLocalDate())) {

                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

                LocalDateTime segmentStart =
                        effectiveStart.isAfter(dayStart)
                                ? effectiveStart
                                : dayStart;

                LocalDateTime segmentEnd =
                        effectiveEnd.isBefore(dayEnd)
                                ? effectiveEnd
                                : dayEnd;

                if (segmentEnd.isAfter(segmentStart)) {

                    long seconds = ChronoUnit.SECONDS.between(
                            segmentStart,
                            segmentEnd
                    );

                    BigDecimal minutes = BigDecimal.valueOf(seconds)
                            .divide(
                                    BigDecimal.valueOf(60),
                                    2,
                                    RoundingMode.HALF_UP
                            );

                    dailyMinutes.merge(
                            date.getDayOfWeek(),
                            minutes,
                            BigDecimal::add
                    );
                }

                date = date.plusDays(1);
            }
        }

        BigDecimal maxMinutes = dailyMinutes.values()
                .stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<PeakUsageDTO> results = new ArrayList<>();

        for (DayOfWeek day : DayOfWeek.values()) {

            String period = day.getDisplayName(
                    TextStyle.FULL,
                    Locale.ENGLISH
            );

            BigDecimal minutes = dailyMinutes.get(day);

            results.add(new PeakUsageDTO(
                    period,
                    minutes,
                    maxMinutes.compareTo(BigDecimal.ZERO) > 0
                            && minutes.compareTo(maxMinutes) == 0
            ));
        }

        return results;
    }

    // ==================================================
    // TOP FIVE DAY-HOUR SLOTS
    // ==================================================

    @Transactional(readOnly = true)
    public List<PeakDayHourDTO> calculateTopFiveDayHourSlots(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime localStart = startDate.atStartOfDay();
        LocalDateTime localEnd = endDate.plusDays(1).atStartOfDay();

        LocalDateTime utcStart = convertPlatformToUtc(localStart);
        LocalDateTime utcEnd = convertPlatformToUtc(localEnd);

        LocalDateTime localNow = convertUtcToPlatform(
                LocalDateTime.now(ZoneOffset.UTC)
        );

        List<Equipment> equipmentList = getAccessibleEquipment();

        if (equipmentList.isEmpty()) {
            return List.of();
        }

        List<Long> equipmentIds = equipmentList.stream()
                .map(Equipment::getId)
                .toList();

        List<EquipmentOperatingHours> allSchedules =
                operatingHoursRepository.findAllByEquipment_IdIn(
                        equipmentIds
                );

        Map<Long, List<EquipmentOperatingHours>> schedulesByEquipment =
                allSchedules.stream()
                        .collect(Collectors.groupingBy(
                                hours -> hours.getEquipment().getId()
                        ));

        Map<DayOfWeek, Map<Integer, BigDecimal>> usedMinutes =
                new EnumMap<>(DayOfWeek.class);

        Map<DayOfWeek, Map<Integer, BigDecimal>> availableMinutes =
                new EnumMap<>(DayOfWeek.class);

        for (DayOfWeek day : DayOfWeek.values()) {

            Map<Integer, BigDecimal> usedHours = new HashMap<>();
            Map<Integer, BigDecimal> availableHours = new HashMap<>();

            for (int hour = 0; hour < 24; hour++) {
                usedHours.put(hour, BigDecimal.ZERO);
                availableHours.put(hour, BigDecimal.ZERO);
            }

            usedMinutes.put(day, usedHours);
            availableMinutes.put(day, availableHours);
        }

        // Calculate available operating minutes.
        for (LocalDate date = startDate;
             !date.isAfter(endDate);
             date = date.plusDays(1)) {

            DayOfWeek day = date.getDayOfWeek();

            for (Equipment equipment : equipmentList) {

                List<EquipmentOperatingHours> schedule =
                        schedulesByEquipment.getOrDefault(
                                equipment.getId(),
                                List.of()
                        );

                List<EquipmentOperatingHours> daySchedule =
                        getScheduleForDay(day, schedule);

                for (EquipmentOperatingHours hours : daySchedule) {

                    LocalDateTime opening =
                            date.atTime(hours.getOpenTime());

                    LocalDateTime closing =
                            date.atTime(hours.getCloseTime());

                    for (int hour = 0; hour < 24; hour++) {

                        LocalDateTime hourStart = date.atTime(hour, 0);
                        LocalDateTime hourEnd = hourStart.plusHours(1);

                        LocalDateTime overlapStart =
                                opening.isAfter(hourStart)
                                        ? opening
                                        : hourStart;

                        LocalDateTime overlapEnd =
                                closing.isBefore(hourEnd)
                                        ? closing
                                        : hourEnd;

                        if (overlapEnd.isAfter(overlapStart)) {

                            long seconds = Duration.between(
                                    overlapStart,
                                    overlapEnd
                            ).getSeconds();

                            BigDecimal minutes = BigDecimal.valueOf(seconds)
                                    .divide(
                                            BigDecimal.valueOf(60),
                                            2,
                                            RoundingMode.HALF_UP
                                    );

                            availableMinutes.get(day).merge(
                                    hour,
                                    minutes,
                                    BigDecimal::add
                            );
                        }
                    }
                }
            }
        }

        List<UsageLog> usageLogs = filterUsageLogs(
                usageLogRepository.findOverlappingUsageLogs(
                        utcStart,
                        utcEnd
                ),
                equipmentList
        );

        // Calculate actual usage only within operating hours.
        for (UsageLog log : usageLogs) {

            if (log.getEquipment() == null
                    || log.getEquipment().getId() == null
                    || log.getActualStartTime() == null) {
                continue;
            }

            Long equipmentId = log.getEquipment().getId();

            List<EquipmentOperatingHours> schedule =
                    schedulesByEquipment.getOrDefault(
                            equipmentId,
                            List.of()
                    );

            LocalDateTime usageStart =
                    convertUtcToPlatform(log.getActualStartTime());

            LocalDateTime usageEnd =
                    log.getActualEndTime() == null
                            ? localNow
                            : convertUtcToPlatform(log.getActualEndTime());

            LocalDateTime effectiveStart =
                    usageStart.isBefore(localStart)
                            ? localStart
                            : usageStart;

            LocalDateTime effectiveEnd =
                    usageEnd.isAfter(localEnd)
                            ? localEnd
                            : usageEnd;

            if (effectiveEnd.isAfter(localNow)) {
                effectiveEnd = localNow;
            }

            if (!effectiveEnd.isAfter(effectiveStart)) {
                continue;
            }

            LocalDateTime cursor = effectiveStart;

            while (cursor.isBefore(effectiveEnd)) {

                DayOfWeek day = cursor.getDayOfWeek();
                LocalDate date = cursor.toLocalDate();

                LocalDateTime nextHour = cursor
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .plusHours(1);

                LocalDateTime segmentEnd =
                        nextHour.isBefore(effectiveEnd)
                                ? nextHour
                                : effectiveEnd;

                List<EquipmentOperatingHours> daySchedule =
                        getScheduleForDay(day, schedule);

                for (EquipmentOperatingHours hours : daySchedule) {

                    LocalDateTime opening =
                            date.atTime(hours.getOpenTime());

                    LocalDateTime closing =
                            date.atTime(hours.getCloseTime());

                    LocalDateTime overlapStart =
                            cursor.isAfter(opening)
                                    ? cursor
                                    : opening;

                    LocalDateTime overlapEnd =
                            segmentEnd.isBefore(closing)
                                    ? segmentEnd
                                    : closing;

                    if (overlapEnd.isAfter(overlapStart)) {

                        long seconds = ChronoUnit.SECONDS.between(
                                overlapStart,
                                overlapEnd
                        );

                        BigDecimal minutes = BigDecimal.valueOf(seconds)
                                .divide(
                                        BigDecimal.valueOf(60),
                                        2,
                                        RoundingMode.HALF_UP
                                );

                        usedMinutes.get(day).merge(
                                cursor.getHour(),
                                minutes,
                                BigDecimal::add
                        );
                    }
                }

                cursor = segmentEnd;
            }
        }

        List<PeakDayHourDTO> results = new ArrayList<>();

        for (DayOfWeek day : DayOfWeek.values()) {

            for (int hour = 0; hour < 24; hour++) {

                BigDecimal used = usedMinutes.get(day).get(hour);
                BigDecimal available = availableMinutes.get(day).get(hour);

                if (used.compareTo(BigDecimal.ZERO) <= 0
                        || available.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal percentage = used
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                available,
                                2,
                                RoundingMode.HALF_UP
                        );

                String dayName = day.getDisplayName(
                        TextStyle.FULL,
                        Locale.ENGLISH
                );

                String hourRange = String.format(
                        "%02d:00-%02d:00",
                        hour,
                        (hour + 1) % 24
                );

                results.add(new PeakDayHourDTO(
                        dayName,
                        hourRange,
                        used,
                        percentage
                ));
            }
        }

        results.sort(
                Comparator
                        .comparing(
                                PeakDayHourDTO::getUtilizationPercentage,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(
                                PeakDayHourDTO::getUsageMinutes,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(
                                dto -> DayOfWeek.valueOf(
                                        dto.getDay().toUpperCase(Locale.ENGLISH)
                                ).getValue()
                        )
                        .thenComparing(
                                dto -> Integer.parseInt(
                                        dto.getHour().substring(0, 2)
                                )
                        )
        );

        return results.stream()
                .limit(5)
                .toList();
    }

    // ==================================================
    // EQUIPMENT HEATMAP
    // ==================================================

    @Transactional(readOnly = true)
    public List<EquipmentHeatmapDTO> calculateEquipmentHeatmap(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        LocalDateTime utcStartDateTime =
                convertPlatformToUtc(startDateTime);

        LocalDateTime utcEndDateTime =
                convertPlatformToUtc(endDateTime);

        List<Equipment> equipmentList = getAccessibleEquipment();

        if (equipmentList.isEmpty()) {
            return List.of();
        }

        List<UsageLog> usageLogs = filterUsageLogs(
                usageLogRepository.findOverlappingUsageLogs(
                        utcStartDateTime,
                        utcEndDateTime
                ),
                equipmentList
        );

        Map<Long, List<EquipmentOperatingHours>> scheduleMap =
                loadOperatingSchedules(equipmentList);

        // Use platform timezone consistently for all heatmap calculations.
        LocalDateTime now = convertUtcToPlatform(
                LocalDateTime.now(ZoneOffset.UTC)
        );

        List<EquipmentHeatmapDTO> results = new ArrayList<>();

        for (Equipment equipment : equipmentList) {

            Long equipmentId = equipment.getId();

            List<EquipmentOperatingHours> schedule =
                    scheduleMap.getOrDefault(
                            equipmentId,
                            List.of()
                    );

            // Convert this equipment's usage logs into platform-local time.
            List<UsageLog> equipmentLogs = usageLogs.stream()
                    .filter(log ->
                            log.getEquipment() != null
                                    && Objects.equals(
                                    log.getEquipment().getId(),
                                    equipmentId
                            )
                                    && log.getActualStartTime() != null
                    )
                    .toList();

            Map<String, Map<String, EquipmentHeatmapSlotDTO>> hourlyUsage =
                    new LinkedHashMap<>();

            for (DayOfWeek day : DayOfWeek.values()) {

                String dayName = day.getDisplayName(
                        TextStyle.FULL,
                        Locale.ENGLISH
                );

                Map<String, EquipmentHeatmapSlotDTO> dayUsage =
                        new LinkedHashMap<>();

                for (int hour = 0; hour < 24; hour++) {

                    String period = String.format("%02d:00", hour);

                    BigDecimal availableMinutes = BigDecimal.ZERO;
                    BigDecimal usedMinutes = BigDecimal.ZERO;

                    // Aggregate this weekday/hour across selected dates.
                    for (LocalDate date = startDate;
                         !date.isAfter(endDate);
                         date = date.plusDays(1)) {

                        if (date.getDayOfWeek() != day) {
                            continue;
                        }

                        LocalDateTime hourStart = date.atTime(hour, 0);
                        LocalDateTime hourEnd = hourStart.plusHours(1);

                        // Available minutes within operating hours.
                        for (EquipmentOperatingHours operatingHours :
                                getScheduleForDay(day, schedule)) {

                            LocalDateTime opening =
                                    date.atTime(operatingHours.getOpenTime());

                            LocalDateTime closing =
                                    date.atTime(operatingHours.getCloseTime());

                            LocalDateTime overlapStart =
                                    hourStart.isAfter(opening)
                                            ? hourStart
                                            : opening;

                            LocalDateTime overlapEnd =
                                    hourEnd.isBefore(closing)
                                            ? hourEnd
                                            : closing;

                            if (overlapEnd.isAfter(overlapStart)) {

                                long seconds = ChronoUnit.SECONDS.between(
                                        overlapStart,
                                        overlapEnd
                                );

                                availableMinutes = availableMinutes.add(
                                        BigDecimal.valueOf(seconds)
                                                .divide(
                                                        BigDecimal.valueOf(60),
                                                        2,
                                                        RoundingMode.HALF_UP
                                                )
                                );
                            }
                        }

                        // Actual usage minutes within this exact date/hour.
                        for (UsageLog log : equipmentLogs) {

                            LocalDateTime sessionStart =
                                    convertUtcToPlatform(
                                            log.getActualStartTime()
                                    );

                            LocalDateTime sessionEnd =
                                    log.getActualEndTime() == null
                                            ? now
                                            : convertUtcToPlatform(
                                            log.getActualEndTime()
                                    );

                            LocalDateTime effectiveStart =
                                    sessionStart.isAfter(hourStart)
                                            ? sessionStart
                                            : hourStart;

                            LocalDateTime effectiveEnd =
                                    sessionEnd.isBefore(hourEnd)
                                            ? sessionEnd
                                            : hourEnd;

                            // Do not count future usage.
                            if (effectiveEnd.isAfter(now)) {
                                effectiveEnd = now;
                            }

                            if (!effectiveEnd.isAfter(effectiveStart)) {
                                continue;
                            }

                            // Restrict usage to the equipment's opening hours.
                            for (EquipmentOperatingHours operatingHours :
                                    getScheduleForDay(day, schedule)) {

                                LocalDateTime opening =
                                        date.atTime(operatingHours.getOpenTime());

                                LocalDateTime closing =
                                        date.atTime(operatingHours.getCloseTime());

                                LocalDateTime overlapStart =
                                        effectiveStart.isAfter(opening)
                                                ? effectiveStart
                                                : opening;

                                LocalDateTime overlapEnd =
                                        effectiveEnd.isBefore(closing)
                                                ? effectiveEnd
                                                : closing;

                                if (overlapEnd.isAfter(overlapStart)) {

                                    long seconds = ChronoUnit.SECONDS.between(
                                            overlapStart,
                                            overlapEnd
                                    );

                                    usedMinutes = usedMinutes.add(
                                            BigDecimal.valueOf(seconds)
                                                    .divide(
                                                            BigDecimal.valueOf(60),
                                                            2,
                                                            RoundingMode.HALF_UP
                                                    )
                                    );
                                }
                            }
                        }
                    }

                    if (availableMinutes.compareTo(BigDecimal.ZERO) == 0) {

                        dayUsage.put(period, null);

                    } else {

                        BigDecimal percentage =
                                usedMinutes
                                        .multiply(BigDecimal.valueOf(100))
                                        .divide(
                                                availableMinutes,
                                                2,
                                                RoundingMode.HALF_UP
                                        );

                        dayUsage.put(
                                period,
                                new EquipmentHeatmapSlotDTO(
                                        availableMinutes.intValue(),
                                        usedMinutes.intValue(),
                                        percentage
                                )
                        );
                    }
                }

                hourlyUsage.put(dayName, dayUsage);
            }

            results.add(new EquipmentHeatmapDTO(
                    equipmentId,
                    equipment.getName(),
                    hourlyUsage
            ));
        }

        return results;
    }

    // ==================================================
    // LOAD OPERATING SCHEDULES IN BULK
    // ==================================================

    private Map<Long, List<EquipmentOperatingHours>> loadOperatingSchedules(
            List<Equipment> equipmentList) {

        List<Long> equipmentIds = equipmentList.stream()
                .map(Equipment::getId)
                .toList();

        if (equipmentIds.isEmpty()) {
            return new HashMap<>();
        }

        List<EquipmentOperatingHours> allHours =
                operatingHoursRepository.findAllByEquipment_IdIn(
                        equipmentIds
                );

        return allHours.stream()
                .collect(Collectors.groupingBy(
                        hours -> hours.getEquipment().getId()
                ));
    }

    // ==================================================
    // CALCULATE USAGE MINUTES
    // ==================================================

    private BigDecimal calculateUsageMinutes(
            Long equipmentId,
            List<UsageLog> usageLogs,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            List<EquipmentOperatingHours> schedule,
            LocalDateTime now) {

        BigDecimal totalMinutes = BigDecimal.ZERO;

        for (UsageLog log : usageLogs) {

            if (log.getEquipment() == null
                    || !log.getEquipment().getId().equals(equipmentId)
                    || log.getActualStartTime() == null) {
                continue;
            }

            LocalDateTime usageStart =
                    convertUtcToPlatform(log.getActualStartTime());

            LocalDateTime usageEnd =
                    log.getActualEndTime() == null
                            ? now
                            : convertUtcToPlatform(log.getActualEndTime());

            totalMinutes = totalMinutes.add(
                    calculateSingleSessionMinutes(
                            usageStart,
                            usageEnd,
                            startDateTime,
                            endDateTime,
                            schedule
                    )
            );
        }

        return totalMinutes;
    }

    // ==================================================
    // CALCULATE ONE SESSION OVERLAP
    // ==================================================

    private BigDecimal calculateSingleSessionMinutes(
            LocalDateTime usageStart,
            LocalDateTime usageEnd,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            List<EquipmentOperatingHours> schedule) {

        LocalDateTime effectiveStart =
                usageStart.isBefore(startDateTime)
                        ? startDateTime
                        : usageStart;

        LocalDateTime effectiveEnd =
                usageEnd.isAfter(endDateTime)
                        ? endDateTime
                        : usageEnd;

        if (!effectiveEnd.isAfter(effectiveStart)) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalMinutes = BigDecimal.ZERO;

        for (LocalDate date = effectiveStart.toLocalDate();
             !date.isAfter(effectiveEnd.toLocalDate());
             date = date.plusDays(1)) {

            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            LocalDateTime sessionStart =
                    effectiveStart.isAfter(dayStart)
                            ? effectiveStart
                            : dayStart;

            LocalDateTime sessionEnd =
                    effectiveEnd.isBefore(dayEnd)
                            ? effectiveEnd
                            : dayEnd;

            if (!sessionEnd.isAfter(sessionStart)) {
                continue;
            }

            for (EquipmentOperatingHours hours :
                    getScheduleForDay(date.getDayOfWeek(), schedule)) {

                LocalDateTime opening =
                        date.atTime(hours.getOpenTime());

                LocalDateTime closing =
                        date.atTime(hours.getCloseTime());

                LocalDateTime overlapStart =
                        sessionStart.isAfter(opening)
                                ? sessionStart
                                : opening;

                LocalDateTime overlapEnd =
                        sessionEnd.isBefore(closing)
                                ? sessionEnd
                                : closing;

                if (overlapEnd.isAfter(overlapStart)) {

                    long seconds = ChronoUnit.SECONDS.between(
                            overlapStart,
                            overlapEnd
                    );

                    BigDecimal minutes = BigDecimal.valueOf(seconds)
                            .divide(
                                    BigDecimal.valueOf(60),
                                    2,
                                    RoundingMode.HALF_UP
                            );

                    totalMinutes = totalMinutes.add(minutes);
                }
            }
        }

        return totalMinutes;
    }

    // ==================================================
    // CALCULATE AVAILABLE OPERATING MINUTES
    // ==================================================


    private BigDecimal calculateAvailableMinutes(
            LocalDate startDate,
            LocalDate endDate,
            List<EquipmentOperatingHours> schedule) {

        BigDecimal totalMinutes = BigDecimal.ZERO;

        LocalDateTime now = convertUtcToPlatform(
                LocalDateTime.now(ZoneOffset.UTC)
        );

        LocalDate effectiveEndDate =
                endDate.isAfter(now.toLocalDate())
                        ? now.toLocalDate()
                        : endDate;

        if (startDate.isAfter(effectiveEndDate)) {
            return BigDecimal.ZERO;
        }

        for (LocalDate date = startDate;
             !date.isAfter(effectiveEndDate);
             date = date.plusDays(1)) {

            for (EquipmentOperatingHours hours :
                    getScheduleForDay(date.getDayOfWeek(), schedule)) {

                LocalDateTime opening =
                        date.atTime(hours.getOpenTime());

                LocalDateTime closing =
                        date.atTime(hours.getCloseTime());

                LocalDateTime effectiveStart = opening;

                LocalDateTime effectiveEnd =
                        closing.isAfter(now) ? now : closing;

                if (!effectiveEnd.isAfter(effectiveStart)) {
                    continue;
                }

                long minutes = Duration.between(
                        effectiveStart,
                        effectiveEnd
                ).toMinutes();

                totalMinutes = totalMinutes.add(
                        BigDecimal.valueOf(minutes)
                );
            }
        }

        return totalMinutes;
    }


    // ==================================================
    // GET OPERATING HOURS FOR A PARTICULAR DAY
    // ==================================================

    private List<EquipmentOperatingHours> getScheduleForDay(
            DayOfWeek day,
            List<EquipmentOperatingHours> schedule) {

        // Default schedule: Monday-Friday, 9 AM-5 PM.
        if (schedule.isEmpty()) {

            if (day.getValue() >= 1 && day.getValue() <= 5) {

                EquipmentOperatingHours defaultHours =
                        new EquipmentOperatingHours();

                defaultHours.setDayOfWeek(day.getValue());
                defaultHours.setOpenTime(LocalTime.of(9, 0));
                defaultHours.setCloseTime(LocalTime.of(17, 0));

                return List.of(defaultHours);
            }

            return List.of();
        }

        // Configured schedule: absent days are closed.
        return schedule.stream()
                .filter(hours ->
                        hours.getDayOfWeek() == day.getValue()
                )
                .toList();
    }

    // ==================================================
    // CONVERT MINUTES INTO HOURS
    // ==================================================

    private BigDecimal toHours(BigDecimal minutes) {

        return minutes.divide(
                BigDecimal.valueOf(60),
                2,
                RoundingMode.HALF_UP
        );
    }

    // ==================================================
    // UTILIZATION PERCENTAGE
    // ==================================================

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

    // ==================================================
    // DATE VALIDATION
    // ==================================================

    private void validateDates(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null
                || endDate == null
                || endDate.isBefore(startDate)) {

            throw new IllegalArgumentException(
                    "Valid start date and end date are required"
            );
        }
    }

    // ==================================================
    // TIMEZONE CONVERSION
    // ==================================================

    private LocalDateTime convertPlatformToUtc(
            LocalDateTime platformTime) {

        return platformTime.atZone(platformTimeZone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    private LocalDateTime convertUtcToPlatform(
            LocalDateTime utcTime) {

        return utcTime.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(platformTimeZone)
                .toLocalDateTime();
    }

    // ==================================================
    // DEPARTMENT AGGREGATION
    // ==================================================

    private static class DepartmentAggregate {

        private final Long departmentId;
        private final String departmentName;

        private BigDecimal actualUsageMinutes = BigDecimal.ZERO;
        private BigDecimal availableMinutes = BigDecimal.ZERO;

        private DepartmentAggregate(
                Long departmentId,
                String departmentName) {

            this.departmentId = departmentId;
            this.departmentName = departmentName;
        }
    }

    // ==================================================
    // INSTITUTION AGGREGATION
    // ==================================================

    private static class InstitutionAggregate {

        private final Long institutionId;
        private final String institutionName;

        private BigDecimal actualUsageMinutes = BigDecimal.ZERO;
        private BigDecimal availableMinutes = BigDecimal.ZERO;

        private InstitutionAggregate(
                Long institutionId,
                String institutionName) {

            this.institutionId = institutionId;
            this.institutionName = institutionName;
        }
    }
}