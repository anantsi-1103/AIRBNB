package com.logic.controller;

import com.logic.DTO.HotelDTO;
import com.logic.Service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels")
@Slf4j
public class HotelController {


    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

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








}
