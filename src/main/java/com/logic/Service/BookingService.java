package com.logic.Service;

import com.logic.DTO.*;

import java.util.List;

public interface BookingService {
//    intializeBooking
    BookingDTO intializeBooking (BookingRequest bookingRequest);
//    Kitne guest ->
    BookingDTO addGuests(Long bookingId , List<GuestDTO> guestDTOList);

    BookingPaymentInitResponseDTO initiatePayments(Long bookingId);

    BookingDTO verifyPayment(Long bookingId, BookingPaymentVerifyRequestDTO paymentVerifyRequest);
}
