package com.logic.Service;

import com.logic.DTO.*;
import com.logic.entity.enums.BookingStatus;

import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.util.List;

public interface BookingService {
//    intializeBooking
    BookingDTO intializeBooking (BookingRequest bookingRequest);
//    Kitne guest ->
    BookingDTO addGuests(Long bookingId , List<GuestDTO> guestDTOList);

    BookingPaymentInitResponseDTO initiatePayments(Long bookingId);

    BookingDTO verifyPayment(Long bookingId, BookingPaymentVerifyRequestDTO paymentVerifyRequest);

    List<BookingDTO> getAllBookingsByHotelId(Long hotelId) throws AccessDeniedException;

    HotelReportDTO getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) throws AccessDeniedException;

    BookingStatus getBookingStatus(Long bookingId);

    List<BookingDTO> getMyBookings();
}
