package com.logic.Service;

import com.logic.DTO.BookingDTO;
import com.logic.DTO.BookingRequest;
import com.logic.DTO.GuestDTO;

import java.util.List;

public interface BookingService {
//    intializeBooking
    BookingDTO intializeBooking (BookingRequest bookingRequest);
//    Kitne guest ->
    BookingDTO addGuests(Long bookingId , List<GuestDTO> guestDTOList);
}
