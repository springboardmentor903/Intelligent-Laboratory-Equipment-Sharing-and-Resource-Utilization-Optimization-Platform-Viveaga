
package com.labresource.platform.controller;

import com.labresource.platform.dto.BookingRequestDTO;
import com.labresource.platform.dto.BookingResponseDTO;
import com.labresource.platform.dto.BookingSuggestionDTO;
import com.labresource.platform.entity.Booking;
import com.labresource.platform.entity.User;
import com.labresource.platform.repository.UserRepository;
import com.labresource.platform.service.BookingService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(
            BookingService bookingService,
            UserRepository userRepository) {

        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    // Create normal or recurring bookings
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<BookingResponseDTO> createBooking(
            @RequestBody BookingRequestDTO request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        if (request.isRecurring()) {

            List<Booking> bookings =
                    bookingService.createRecurringBookings(
                            user.getId(),
                            request.getEquipmentId(),
                            request.getStartTime(),
                            request.getEndTime(),
                            request.getPurpose(),
                            request.getUsageMode(),
                            request.getRecurrenceType(),
                            request.getRecurrenceCount()
                    );

            return bookings.stream()
                    .map(BookingResponseDTO::fromEntity)
                    .toList();
        }

        Booking booking = bookingService.createBooking(
                user.getId(),
                request.getEquipmentId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getPurpose(),
                request.getUsageMode()
        );

        return List.of(BookingResponseDTO.fromEntity(booking));
    }

    // Suggest alternative booking slots
    @GetMapping("/suggestions")
    public List<BookingSuggestionDTO> getBookingSuggestions(
            @RequestParam Long equipmentId,
            @RequestParam LocalDateTime requestedStartTime,
            @RequestParam LocalDateTime requestedEndTime,
            @RequestParam(defaultValue = "5") int suggestionCount,
            Authentication authentication) {

        getAuthenticatedUser(authentication);

        return bookingService.suggestAlternativeSlots(
                equipmentId,
                requestedStartTime,
                requestedEndTime,
                suggestionCount
        );
    }

    // View authenticated user's bookings
    @GetMapping("/my")
    public List<BookingResponseDTO> getMyBookings(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        return bookingService.getBookingsByUser(user.getId())
                .stream()
                .map(BookingResponseDTO::fromEntity)
                .toList();
    }

    // View all bookings - LAB_MANAGER only
    @GetMapping("/all")
    public List<BookingResponseDTO> getAllBookings(
            Authentication authentication) {

        requireLabManager(authentication);

        return bookingService.getAllBookings()
                .stream()
                .map(BookingResponseDTO::fromEntity)
                .toList();
    }

    // Cancel user's own pending booking
    @PatchMapping("/{bookingId}/cancel")
    public BookingResponseDTO cancelBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        Booking booking = bookingService.cancelBooking(
                bookingId,
                user.getId()
        );

        return BookingResponseDTO.fromEntity(booking);
    }

    // Approve booking - LAB_MANAGER only
    @PatchMapping("/{bookingId}/approve")
    public BookingResponseDTO approveBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        requireLabManager(authentication);

        Booking booking = bookingService.approveBooking(bookingId);

        return BookingResponseDTO.fromEntity(booking);
    }

    // Reject booking - LAB_MANAGER only
    @PatchMapping("/{bookingId}/reject")
    public BookingResponseDTO rejectBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        requireLabManager(authentication);

        Booking booking = bookingService.rejectBooking(bookingId);

        return BookingResponseDTO.fromEntity(booking);
    }

    // Check in
    @PostMapping("/{bookingId}/check-in")
    public BookingResponseDTO checkIn(
            @PathVariable Long bookingId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        Booking booking = bookingService.checkIn(
                bookingId,
                user.getId()
        );

        return BookingResponseDTO.fromEntity(booking);
    }

    // Check out
    @PostMapping("/{bookingId}/check-out")
    public BookingResponseDTO checkOut(
            @PathVariable Long bookingId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        Booking booking = bookingService.checkOut(
                bookingId,
                user.getId()
        );

        return BookingResponseDTO.fromEntity(booking);
    }

    // Get authenticated user
    private User getAuthenticatedUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Login required"
            );
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));
    }

    // Allow only LAB_MANAGER
    private void requireLabManager(Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        if (user.getRole() == null
                || !"LAB_MANAGER".equals(
                user.getRole().toString())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only Lab Managers can manage bookings"
            );
        }
    }
}
