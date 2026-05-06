package com.logic.controller;

import com.logic.DTO.BookingDTO;
import com.logic.DTO.HotelDTO;
import com.logic.DTO.HotelReportDTO;
import com.logic.Service.BookingService;
import com.logic.Service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/hotels")
@PreAuthorize("hasRole('HOTEL_MANAGER')")
@Slf4j
@RequiredArgsConstructor
public class HotelController {


    private final HotelService hotelService;
    private final BookingService bookingService;

    @PostMapping()
    public ResponseEntity<HotelDTO> createNewHotel(@RequestBody HotelDTO hotelDTO){
        log.info("Attempting to create a new Hotel with name "+ hotelDTO.getName());
        HotelDTO hotel = hotelService.createNewHotel(hotelDTO);
        return new ResponseEntity<>(hotel, HttpStatus.CREATED);

    }

    @GetMapping("/{HotelId}")
    public ResponseEntity<HotelDTO> getHotelByID(@PathVariable Long HotelId){
        HotelDTO hotel = hotelService.getHotelByID(HotelId);
        return  ResponseEntity.ok(hotel);

    }

    @PutMapping("/{HotelId}")
    public ResponseEntity<HotelDTO> updateHotelById(@PathVariable Long HotelId, @RequestBody HotelDTO hotelDTO){
        HotelDTO hotel = hotelService.updateHotelById(HotelId, hotelDTO);
        return  ResponseEntity.ok(hotel);

    }

    @Transactional
    @DeleteMapping("/{HotelId}")
    public ResponseEntity<Void> deleteHotelById(@PathVariable Long HotelId){
        hotelService.deleteHotelById(HotelId);
        return  ResponseEntity.noContent().build();

    }

    @PatchMapping("/{HotelId}")
    public ResponseEntity<Void> activateHotel(@PathVariable Long HotelId){
        hotelService.activateHotel(HotelId);
        return  ResponseEntity.noContent().build();

    }

    @GetMapping
    public ResponseEntity<List<HotelDTO>> getAllHotels() {

        List<HotelDTO> hotels = hotelService.getAllHotels();

        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/{hotelId}/bookings")
    public ResponseEntity<List<BookingDTO>> getAllBookingsByHotelId(@PathVariable Long hotelId) throws AccessDeniedException {
        return ResponseEntity.ok(bookingService.getAllBookingsByHotelId(hotelId));
    }

    @GetMapping("/{hotelId}/reports")
    @Operation(summary = "Generate a bookings report of a hotel", tags = {"Admin Bookings"})
    public ResponseEntity<HotelReportDTO> getHotelReport(@PathVariable Long hotelId,
                                                         @RequestParam(required = false) LocalDate startDate,
                                                         @RequestParam(required = false) LocalDate endDate) throws AccessDeniedException {

        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        return ResponseEntity.ok(bookingService. getHotelReport(hotelId, startDate, endDate));
    }








}
