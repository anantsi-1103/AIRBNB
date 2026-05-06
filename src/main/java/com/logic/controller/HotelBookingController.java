package com.logic.controller;


import com.logic.DTO.*;
import com.logic.Service.BookingService;
import com.logic.Service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class HotelBookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

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

    @PostMapping("/{bookingId}/payments/failed")
    public ResponseEntity<PaymentDTO> recordFailedPayment(
            @PathVariable Long bookingId,
            @RequestBody PaymentFailureRequestDTO failureRequest
    ) {
        return ResponseEntity.ok(paymentService.recordFailedPayment(bookingId, failureRequest));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingDTO> cancelOrRefundBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.cancelOrRefundBooking(bookingId));
    }

    @GetMapping("/{bookingId}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long bookingId) {
        InvoiceDTO invoice = paymentService.buildInvoice(bookingId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + invoice.getFileName() + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(invoice.getContent());
    }

    @GetMapping("/{bookingId}/status")
    public ResponseEntity<BookingStatusResponseDTO> getBookingStatus(@PathVariable Long bookingId) {
        return ResponseEntity.ok(new BookingStatusResponseDTO(bookingService.getBookingStatus(bookingId)));
    }



}
