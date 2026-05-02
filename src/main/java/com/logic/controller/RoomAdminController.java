package com.logic.controller;


import com.logic.DTO.InventoryUpdateRequest;
import com.logic.DTO.RoomDTO;
import com.logic.Service.InventoryService;
import com.logic.Service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels/{hotelId}/rooms")
@PreAuthorize("hasRole('HOTEL_MANAGER')")
@RequiredArgsConstructor
public class RoomAdminController {

    private final RoomService roomService;
    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<RoomDTO> createNewRoom(@PathVariable Long hotelId,
                                                 @RequestBody RoomDTO roomDTO){
        RoomDTO room = roomService.createNewRoom(hotelId,roomDTO);
        return new ResponseEntity<>(room, HttpStatus.CREATED);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDTO> getRoomById(@PathVariable Long hotelId,
                                               @PathVariable Long roomId){
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    @GetMapping
    public ResponseEntity<List<RoomDTO>> getAllRoomInHotel(@PathVariable Long hotelId ){
        return ResponseEntity.ok(roomService.getAllRoomInHotel(hotelId));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<RoomDTO> deleteRoomById(@PathVariable Long hotelId,@PathVariable Long roomId){
        roomService.deleteRoomById(roomId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{roomId}/inventory")
    public ResponseEntity<Void> updateRoomInventory(@PathVariable Long hotelId,
                                                    @PathVariable Long roomId,
                                                    @RequestBody InventoryUpdateRequest inventoryUpdateRequest){
        inventoryService.updateRoomInventory(roomId, inventoryUpdateRequest);
        return ResponseEntity.noContent().build();
    }

}
