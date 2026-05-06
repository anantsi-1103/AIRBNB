package com.logic.Service;

import com.logic.DTO.*;
import com.logic.Repository.HotelRepository;
import com.logic.Repository.InventoryRepository;
import com.logic.entity.Hotel;
import com.logic.entity.Room;
import com.logic.entity.User;
import com.logic.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.logic.utils.AppUtils.getCurrentUser;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService{

    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;


    @Override
    public HotelDTO createNewHotel(HotelDTO hotelDTO) {
        log.info("Creating a new Hotel with name : {}", hotelDTO.getName());
        Hotel hotel = modelMapper.map(hotelDTO,Hotel.class);
        hotel.setActive(false);
        hotel.setOwner(getCurrentUser());
        hotel = hotelRepository.save(hotel);
        log.info("Created a new Hotel with ID : {}", hotelDTO.getId());
        return modelMapper.map(hotel, HotelDTO.class);
    }

    @Override
    public HotelDTO getHotelByID(Long id) {
        log.info("Getting Hotel details by ID {}", id);
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID :"+id));
        return modelMapper.map(hotel,HotelDTO.class);

    }

    @Override
    public HotelDTO updateHotelById(Long id, HotelDTO hotelDTO) {
        log.info("Updating Hotel details by ID {}", id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Hotel not found with this ID" +id));
        modelMapper.map(hotelDTO , hotel);
        hotel.setId(id);
        hotel = hotelRepository.save(hotel);
        return modelMapper.map(hotel, HotelDTO.class);
    }

    @Override
    @Transactional
    public void deleteHotelById(Long id) {
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID :"+id));

//        TODO -> Inventory
        for(Room room: hotel.getRooms()) {
            inventoryService.deleteAllInventories(room);
        }
        hotelRepository.deleteById(id);
    }

    @Transactional
    @Override
    public void activateHotel(Long hotelID) {
        log.info("Activationg Hotel details by ID {}", hotelID);
        Hotel hotel = hotelRepository.findById(hotelID).orElseThrow(()-> new ResourceNotFoundException("Hotel not found with this ID" +hotelID));

        hotel.setActive(true);
        hotelRepository.save(hotel);
//       TODO -> Inventory update ->
        for(Room room: hotel.getRooms()) {
            inventoryService.initializeRoomForAYear(room);
        }
    }

    @Override
    public List<HotelDTO> getAllHotels() {
        User user = getCurrentUser();
        log.info("Getting all hotels for the admin user with ID: {}", user.getId());
        List<Hotel> hotels = hotelRepository.findByOwner(user);

        return hotels
                .stream()
                .map((element) -> modelMapper.map(element, HotelDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public HotelInfoDTO getHotelInfoById(Long hotelId) {
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: "+hotelId));

        List<RoomPriceResponseDTO> rooms = hotel.getRooms()
                .stream()
                .map((element) -> modelMapper.map(element, RoomPriceResponseDTO.class))
                .toList();

        // hotel ke data ko DTO Map krdunga
        return new HotelInfoDTO(modelMapper.map(hotel, HotelDTO.class), rooms);
    }

    @Override
    public HotelInfoDTO getHotelInfoById(Long hotelId, HotelInfoRequestDTO hotelInfoRequestDTO) {
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        if (hotelInfoRequestDTO == null
                || hotelInfoRequestDTO.getStartDate() == null
                || hotelInfoRequestDTO.getEndDate() == null
                || hotelInfoRequestDTO.getRoomsCount() == null
                || hotelInfoRequestDTO.getRoomsCount() <= 0) {
            return getHotelInfoById(hotelId);
        }

        long daysCount = ChronoUnit.DAYS.between(hotelInfoRequestDTO.getStartDate(), hotelInfoRequestDTO.getEndDate());
        if (daysCount <= 0) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        List<RoomPriceResponseDTO> rooms = inventoryRepository.findRoomAveragePrice(
                        hotelId,
                        hotelInfoRequestDTO.getStartDate(),
                        hotelInfoRequestDTO.getEndDate(),
                        hotelInfoRequestDTO.getRoomsCount(),
                        daysCount
                )
                .stream()
                .map(roomPriceDTO -> {
                    RoomPriceResponseDTO room = modelMapper.map(roomPriceDTO.getRoom(), RoomPriceResponseDTO.class);
                    room.setPrice(roomPriceDTO.getPrice());
                    return room;
                })
                .toList();

        return new HotelInfoDTO(modelMapper.map(hotel, HotelDTO.class), rooms);
    }



}
