package com.logic.controller;


import com.logic.DTO.HotelDTO;
import com.logic.DTO.HotelInfoDTO;
import com.logic.DTO.HotelSearchRequest;
import com.logic.Service.HotelService;
import com.logic.Service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelBrowseController {
    private final InventoryService inventoryService;
    private final HotelService hotelService;

    @PostMapping("/search")
    public ResponseEntity<Page<HotelDTO>> searchHotels(
            @RequestBody HotelSearchRequest hotelSearchRequest) {

        Page<HotelDTO> page =
                inventoryService.searchHotels(hotelSearchRequest);

        return ResponseEntity.ok(page);
    }

    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDTO> getHotelInfo(@PathVariable Long hotelId) {
        return ResponseEntity.ok(hotelService.getHotelInfoById(hotelId));
    }
}
