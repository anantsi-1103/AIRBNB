package com.logic.controller;


import com.logic.DTO.BookingDTO;
import com.logic.DTO.BookingRequest;
import com.logic.DTO.GuestDTO;
import com.logic.DTO.HotelInfoDTO;
import com.logic.Service.BookingService;
import com.logic.entity.Guest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
