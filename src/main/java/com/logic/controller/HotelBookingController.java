package com.logic.controller;


import com.logic.DTO.*;
import com.logic.Service.BookingService;
import com.logic.entity.Guest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class HotelBookingController {

    private final BookingService bookingService;

    @PostMapping("/init")
    public ResponseEntity<BookingDTO> initialiseBooking(@RequestBody BookingRequest bookingRequest){
        return ResponseEntity.ok(bookingService.intializeBooking(bookingRequest));
    }

    @PostMapping("{bookingId}/guest")
    public ResponseEntity<BookingDTO> addGuest(@PathVariable Long bookingId, @RequestBody List<GuestDTO> guestDTOList){
        return ResponseEntity.ok(bookingService.addGuests(bookingId,guestDTOList));
    }

    @PostMapping("/{bookingId}/payments")
    public ResponseEntity<BookingPaymentInitResponseDTO> initiatePayment(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.initiatePayments(bookingId));
    }

    @PostMapping("/{bookingId}/payments/verify")
    public ResponseEntity<BookingDTO> verifyPayment(@PathVariable Long bookingId,
                                                    @RequestBody BookingPaymentVerifyRequestDTO paymentVerifyRequest) {
        return ResponseEntity.ok(bookingService.verifyPayment(bookingId, paymentVerifyRequest));
    }




}
