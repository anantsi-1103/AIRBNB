package com.logic.Service;

import com.logic.DTO.RoomDTO;
import com.logic.Repository.HotelRepository;
import com.logic.Repository.RoomRepository;
import com.logic.entity.Hotel;
import com.logic.entity.Room;
import com.logic.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService{

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;

    @Override
    public RoomDTO createNewRoom(Long hotelId, RoomDTO roomDTO) {
        log.info("Creating a new room in the Hotel with ID : {}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID :"+hotelId));

        Room room = modelMapper.map(roomDTO,Room.class);
        room.setHotel(hotel);
        room = roomRepository.save(room);

    // inventory TODO - inventory -> available ->
        if (hotel.getActive()) {
            inventoryService.initializeRoomForAYear(room);
        }

        return modelMapper.map(room, RoomDTO.class);
    }

    @Override
    public List<RoomDTO> getAllRoomInHotel(Long hotelId) {
        log.info("Get all room in the Hotel by ID : {}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID :"+hotelId));

        return hotel
                .getRooms()
                .stream()
                .map((element) -> modelMapper.map(element, RoomDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public RoomDTO getRoomById(Long roomId) {
        log.info("Get room  by ID : {}", roomId);
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID :"+roomId));

        return modelMapper.map(room, RoomDTO.class);



    }

    @Transactional
    @Override
    public void deleteRoomById(Long roomId) {

        log.info("Deleting the room with ID: {}", roomId);

        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Room not found with ID: " + roomId));

        roomRepository.delete(room);

        log.info("Room deleted successfully: {}", roomId);
    }
}
