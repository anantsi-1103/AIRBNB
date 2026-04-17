package com.logic.Service;

import com.logic.DTO.RoomDTO;
import com.logic.Repository.HotelRepo;
import com.logic.Repository.RoomRepo;
import com.logic.entity.Hotel;
import com.logic.entity.Room;
import com.logic.exception.ResourceNotFoundException;
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

    private final RoomRepo roomRepo;
    private final HotelRepo hotelRepo;
    private final ModelMapper modelMapper;




    @Override
    public RoomDTO createNewRoom(Long hotelId, RoomDTO roomDTO) {
        log.info("Creating a new room in the Hotel with ID : {}", hotelId);
        Hotel hotel = hotelRepo
                .findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID :"+hotelId));

        Room room = modelMapper.map(roomDTO,Room.class);
        room.setHotel(hotel);
        room = roomRepo.save(room);

    // inventory TODO - inventory -> available ->
        return modelMapper.map(room, RoomDTO.class);
    }

    @Override
    public List<RoomDTO> getAllRoomInHotel(Long hotelId) {
        log.info("Get all room in the Hotel by ID : {}", hotelId);
        Hotel hotel = hotelRepo
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
        Room room = roomRepo
                .findById(roomId)
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID :"+roomId));

        return modelMapper.map(room, RoomDTO.class);



    }

    @Override
    public void deleteRoomById(Long roomId) {
        log.info("Deleting the room  by ID : {}", roomId);
        Room room = roomRepo
                .findById(roomId)
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID :"+roomId));

//        TODO -> Inventory m bhi delete krdenge us room ko
        roomRepo.deleteById(roomId);
    }
}
