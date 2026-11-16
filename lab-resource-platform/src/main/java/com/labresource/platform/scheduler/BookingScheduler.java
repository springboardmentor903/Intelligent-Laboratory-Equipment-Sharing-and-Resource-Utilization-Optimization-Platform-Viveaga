
package com.labresource.platform.scheduler;

import com.labresource.platform.entity.Booking;
import com.labresource.platform.entity.BookingStatus;
import com.labresource.platform.repository.BookingRepository;
import com.labresource.platform.service.WaitlistService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class BookingScheduler {

    private final BookingRepository bookingRepository;
    private final WaitlistService waitlistService;

    public BookingScheduler(
            BookingRepository bookingRepository,
            WaitlistService waitlistService) {

        this.bookingRepository = bookingRepository;
        this.waitlistService = waitlistService;
    }

    // Check for bookings without check-in every 60 seconds.
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void markNoShowBookings() {

        // Booking times are stored in UTC.
        LocalDateTime now =
                LocalDateTime.now(ZoneOffset.UTC);

        // Allow check-in until 15 minutes after booking start.
        LocalDateTime checkInDeadline =
                now.minusMinutes(15);

        List<Booking> bookings =
                bookingRepository.findByStatusAndStartTimeBefore(
                        BookingStatus.APPROVED,
                        checkInDeadline
                );

        for (Booking booking : bookings) {

            // Mark booking as NO_SHOW.
            booking.setStatus(BookingStatus.NO_SHOW);
            bookingRepository.save(booking);

            // Release only the remaining interval.
            if (booking.getEndTime().isAfter(now)) {

                waitlistService.offerNextWaitlistUser(
                        booking.getEquipment().getId(),
                        now,
                        booking.getEndTime()
                );
            }
        }
    }
}
