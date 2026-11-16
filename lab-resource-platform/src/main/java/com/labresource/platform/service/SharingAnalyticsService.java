
package com.labresource.platform.service;

import com.labresource.platform.entity.*;
import com.labresource.platform.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class SharingAnalyticsService {

    private final BookingRepository bookingRepository;
    private final UsageLogRepository usageLogRepository;

    public SharingAnalyticsService(
            BookingRepository bookingRepository,
            UsageLogRepository usageLogRepository) {

        this.bookingRepository = bookingRepository;
        this.usageLogRepository = usageLogRepository;
    }

    // Only valid sharing bookings are included.
    // NO_SHOW, PENDING, REJECTED and CANCELLED are excluded.
    private List<Booking> getValidSharingBookings() {

        return bookingRepository.findAll()
                .stream()
                .filter(booking -> booking.getSharing() != null)
                .filter(this::isValidBooking)
                .toList();
    }

    private boolean isValidBooking(Booking booking) {

        if (booking.getStatus() == null) {
            return false;
        }

        String status = booking.getStatus().name();

        return status.equals("APPROVED")
                || status.equals("COMPLETED");
    }

    private boolean belongsToInstitution(
            Booking booking,
            Long institutionId) {

        if (institutionId == null) {
            return true;
        }

        Long ownerId = booking.getSharing()
                .getInstitution()
                .getId();

        Long requesterId = booking.getUser()
                .getInstitution()
                .getId();

        return institutionId.equals(ownerId)
                || institutionId.equals(requesterId);
    }

    // Scheduled booking duration
    private double bookedHours(Booking booking) {

        if (booking.getStartTime() == null
                || booking.getEndTime() == null) {
            return 0;
        }

        long minutes = Duration.between(
                booking.getStartTime(),
                booking.getEndTime()
        ).toMinutes();

        return minutes > 0 ? minutes / 60.0 : 0;
    }

    // Actual equipment usage duration
    private double actualHours(Booking booking) {

        if (booking.getId() == null) {
            return 0;
        }

        List<UsageLog> logs =
                usageLogRepository.findByBookingId(booking.getId());

        double totalMinutes = 0;

        for (UsageLog log : logs) {

            if (log.getActualStartTime() == null) {
                continue;
            }

            LocalDateTime end = log.getActualEndTime() == null
                    ? LocalDateTime.now()
                    : log.getActualEndTime();

            if (end.isBefore(log.getActualStartTime())) {
                continue;
            }

            totalMinutes += Duration.between(
                    log.getActualStartTime(),
                    end
            ).toMinutes();
        }

        return totalMinutes / 60.0;
    }

    private double fee(double hours, Double hourlyCost) {

        return hours * (hourlyCost == null ? 0 : hourlyCost);
    }

    private double hourlyCost(Booking booking) {

        return booking.getSharing().getHourlyCost() == null
                ? 0
                : booking.getSharing().getHourlyCost();
    }

    // 1. SUMMARY

    public Map<String, Object> getSummary(Long institutionId) {

        double providedHours = 0;
        double usedHours = 0;
        double feesEarned = 0;
        double feesOwed = 0;

        for (Booking booking : getValidSharingBookings()) {

            if (!belongsToInstitution(booking, institutionId)) {
                continue;
            }

            Long ownerId = booking.getSharing()
                    .getInstitution()
                    .getId();

            Long requesterId = booking.getUser()
                    .getInstitution()
                    .getId();

            double booked = bookedHours(booking);
            double used = actualHours(booking);
            double rate = hourlyCost(booking);

            if (institutionId == null
                    || institutionId.equals(ownerId)) {

                providedHours += booked;
                feesEarned += fee(used, rate);
            }

            if (institutionId == null
                    || institutionId.equals(requesterId)) {

                usedHours += used;
                feesOwed += fee(used, rate);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("providedHours", providedHours);
        result.put("usedHours", usedHours);
        result.put("feesEarned", feesEarned);
        result.put("feesOwed", feesOwed);

        return result;
    }

    // 2. MONTHLY TREND

    public List<Map<String, Object>> getTrend(Long institutionId) {

        Map<String, Map<String, Object>> monthly = new TreeMap<>();

        for (Booking booking : getValidSharingBookings()) {

            if (!belongsToInstitution(booking, institutionId)
                    || booking.getStartTime() == null) {
                continue;
            }

            String key = YearMonth.from(
                    booking.getStartTime()
            ).toString();

            Map<String, Object> data = monthly.computeIfAbsent(
                    key,
                    k -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("month", k);
                        row.put("providedHours", 0.0);
                        row.put("usedHours", 0.0);
                        row.put("feesEarned", 0.0);
                        row.put("feesOwed", 0.0);
                        return row;
                    }
            );

            Long ownerId = booking.getSharing()
                    .getInstitution()
                    .getId();

            Long requesterId = booking.getUser()
                    .getInstitution()
                    .getId();

            double booked = bookedHours(booking);
            double used = actualHours(booking);
            double rate = hourlyCost(booking);

            if (institutionId == null
                    || institutionId.equals(ownerId)) {

                data.put(
                        "providedHours",
                        (double) data.get("providedHours") + booked
                );

                data.put(
                        "feesEarned",
                        (double) data.get("feesEarned")
                                + fee(used, rate)
                );
            }

            if (institutionId == null
                    || institutionId.equals(requesterId)) {

                data.put(
                        "usedHours",
                        (double) data.get("usedHours") + used
                );

                data.put(
                        "feesOwed",
                        (double) data.get("feesOwed")
                                + fee(used, rate)
                );
            }
        }

        return new ArrayList<>(monthly.values());
    }

    // 3. PARTNER INSTITUTIONS

    public List<Map<String, Object>> getPartners(Long institutionId) {

        Map<String, Map<String, Object>> partners =
                new LinkedHashMap<>();

        for (Booking booking : getValidSharingBookings()) {

            if (!belongsToInstitution(booking, institutionId)) {
                continue;
            }

            Institution owner = booking.getSharing().getInstitution();

            Institution requester = booking.getUser().getInstitution();

            Institution partner = institutionId == null
                    ? requester
                    : institutionId.equals(owner.getId())
                      ? requester
                      : owner;

            // For system administrator, distinguish each owner-requester pair.
            String key = institutionId == null
                    ? owner.getId() + "-" + requester.getId()
                    : partner.getId().toString();

            Map<String, Object> data = partners.computeIfAbsent(
                    key,
                    k -> {
                        Map<String, Object> row = new LinkedHashMap<>();

                        row.put("partnerId", partner.getId());
                        row.put("partnerName", partner.getName());
                        row.put("bookingCount", 0);
                        row.put("hours", 0.0);
                        row.put("fees", 0.0);

                        if (institutionId == null) {
                            row.put("ownerInstitutionId", owner.getId());
                            row.put("ownerInstitutionName", owner.getName());
                        }

                        return row;
                    }
            );

            double used = actualHours(booking);
            double rate = hourlyCost(booking);

            data.put(
                    "bookingCount",
                    (int) data.get("bookingCount") + 1
            );

            data.put(
                    "hours",
                    (double) data.get("hours") + used
            );

            data.put(
                    "fees",
                    (double) data.get("fees") + fee(used, rate)
            );
        }

        return new ArrayList<>(partners.values());
    }

    // 4. EQUIPMENT-WISE ANALYTICS

    public List<Map<String, Object>> getEquipment(Long institutionId) {

        Map<Long, Map<String, Object>> equipmentData =
                new LinkedHashMap<>();

        for (Booking booking : getValidSharingBookings()) {

            if (!belongsToInstitution(booking, institutionId)) {
                continue;
            }

            Equipment equipment = booking.getSharing().getEquipment();

            Map<String, Object> data = equipmentData.computeIfAbsent(
                    equipment.getId(),
                    id -> {
                        Map<String, Object> row = new LinkedHashMap<>();

                        row.put("equipmentId", equipment.getId());
                        row.put("equipmentName", equipment.getName());
                        row.put("bookingCount", 0);
                        row.put("bookedHours", 0.0);
                        row.put("usedHours", 0.0);
                        row.put("fees", 0.0);

                        return row;
                    }
            );

            double booked = bookedHours(booking);
            double used = actualHours(booking);
            double rate = hourlyCost(booking);

            data.put(
                    "bookingCount",
                    (int) data.get("bookingCount") + 1
            );

            data.put(
                    "bookedHours",
                    (double) data.get("bookedHours") + booked
            );

            data.put(
                    "usedHours",
                    (double) data.get("usedHours") + used
            );

            data.put(
                    "fees",
                    (double) data.get("fees") + fee(used, rate)
            );
        }

        return new ArrayList<>(equipmentData.values());
    }

    // 5. PARTNERSHIP REPORT

    public Map<String, Object> getPartnerReport(
            Long institutionId,
            Long partnerId) {

        double totalBookedHours = 0;
        double totalUsedHours = 0;
        double totalFees = 0;

        int bookingCount = 0;

        for (Booking booking : getValidSharingBookings()) {

            Long ownerId = booking.getSharing()
                    .getInstitution()
                    .getId();

            Long requesterId = booking.getUser()
                    .getInstitution()
                    .getId();

            boolean matchingPair =
                    (institutionId.equals(ownerId)
                            && partnerId.equals(requesterId))
                            ||
                            (institutionId.equals(requesterId)
                                    && partnerId.equals(ownerId));

            if (!matchingPair) {
                continue;
            }

            double booked = bookedHours(booking);
            double used = actualHours(booking);
            double rate = hourlyCost(booking);

            totalBookedHours += booked;
            totalUsedHours += used;
            totalFees += fee(used, rate);

            bookingCount++;
        }

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("institutionId", institutionId);
        result.put("partnerId", partnerId);
        result.put("bookingCount", bookingCount);
        result.put("bookedHours", totalBookedHours);
        result.put("usedHours", totalUsedHours);
        result.put("fees", totalFees);

        return result;
    }
}
