
package com.labresource.platform.controller;

import com.labresource.platform.dto.BookingRequestDTO;
import com.labresource.platform.dto.BookingResponseDTO;
import com.labresource.platform.entity.Booking;
import com.labresource.platform.entity.User;
import com.labresource.platform.repository.UserRepository;
import com.labresource.platform.service.BookingService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    // Create a booking for the authenticated user
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponseDTO createBooking(
            @RequestBody BookingRequestDTO request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        Booking booking = bookingService.createBooking(
                user.getId(),
                request.getEquipmentId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getPurpose()
        );

        return BookingResponseDTO.fromEntity(booking);
    }

    // View the authenticated user's own bookings
    @GetMapping("/my")
    public List<BookingResponseDTO> getMyBookings(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        return bookingService.getBookingsByUser(user.getId())
                .stream()
                .map(BookingResponseDTO::fromEntity)
                .toList();
    }

    // View every user's bookings - LAB_MANAGER only
    @GetMapping("/all")
    public List<BookingResponseDTO> getAllBookings(
            Authentication authentication) {

        requireLabManager(authentication);

        return bookingService.getAllBookings()
                .stream()
                .map(BookingResponseDTO::fromEntity)
                .toList();
    }

    // Cancel the authenticated user's own pending booking
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

    // Approve a booking - LAB_MANAGER only
    @PatchMapping("/{bookingId}/approve")
    public BookingResponseDTO approveBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        requireLabManager(authentication);

        Booking booking = bookingService.approveBooking(bookingId);

        return BookingResponseDTO.fromEntity(booking);
    }

    // Reject a booking - LAB_MANAGER only
    @PatchMapping("/{bookingId}/reject")
    public BookingResponseDTO rejectBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        requireLabManager(authentication);

        Booking booking = bookingService.rejectBooking(bookingId);

        return BookingResponseDTO.fromEntity(booking);
    }

    // Get the authenticated user from the database
    private User getAuthenticatedUser(
            Authentication authentication) {

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

    // Allow only users whose database role is LAB_MANAGER
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
