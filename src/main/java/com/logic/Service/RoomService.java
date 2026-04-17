package com.logic.Service;

import com.logic.DTO.RoomDTO;
import com.logic.Repository.RoomRepo;

import java.util.List;

public interface RoomService {

    RoomDTO createNewRoom(Long hotelId, RoomDTO roomDTO);

    List<RoomDTO> getAllRoomInHotel(Long hotelId);

    RoomDTO getRoomById(Long roomId);

    void deleteRoomById(Long roomId);

}
