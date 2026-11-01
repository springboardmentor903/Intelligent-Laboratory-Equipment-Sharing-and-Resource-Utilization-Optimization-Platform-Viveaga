package com.labresource.platform.scheduler;

import com.labresource.platform.entity.Booking;
import com.labresource.platform.entity.BookingStatus;

import com.labresource.platform.entity.Booking;
import com.labresource.platform.repository.BookingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookingScheduler {

    private final BookingRepository bookingRepository;

    public BookingScheduler(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void markNoShowBookings() {

        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);

        List<Booking> bookings = bookingRepository
                .findByStatusAndStartTimeBefore(
                        BookingStatus.APPROVED,
                        deadline
                );

        for (Booking booking : bookings) {
            booking.setStatus(BookingStatus.NO_SHOW);
            bookingRepository.save(booking);
        }
    }
}