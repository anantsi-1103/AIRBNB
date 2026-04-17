package com.logic.Service;

import com.logic.DTO.HotelDTO;
import com.logic.Repository.HotelRepo;
import com.logic.entity.Hotel;
import com.logic.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HoteServiceImpl implements HotelService{

    private final HotelRepo hotelRepo;
    private final ModelMapper modelMapper;

    @Override
    public HotelDTO createNewHotel(HotelDTO hotelDTO) {
        log.info("Creating a new Hotel with name : {}", hotelDTO.getName());
        Hotel hotel = modelMapper.map(hotelDTO,Hotel.class);
        hotel.setActive(false);
        hotel = hotelRepo.save(hotel);
        log.info("Created a new Hotel with ID : {}", hotelDTO.getId());
        return modelMapper.map(hotel, HotelDTO.class);
    }

    @Override
    public HotelDTO getHotelByID(Long id) {
        log.info("Getting Hotel details by ID {}", id);
        Hotel hotel = hotelRepo
                .findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID :"+id));
        return modelMapper.map(hotel,HotelDTO.class);

    }

    @Override
    public HotelDTO updateHotelById(Long id, HotelDTO hotelDTO) {
        log.info("Updating Hotel details by ID {}", id);
        Hotel hotel = hotelRepo.findById(id).orElseThrow(()-> new ResourceNotFoundException("Hotel not found with this ID" +id));
        modelMapper.map(hotelDTO , hotel);
        hotel.setId(id);
        hotel = hotelRepo.save(hotel);
        return modelMapper.map(hotel, HotelDTO.class);
    }

    @Override
    @Transactional
    public void deleteHotelById(Long id) {
        Hotel hotel = hotelRepo
                .findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID :"+id));

        hotelRepo.deleteById(id);
//        TODO -> Inventory
    }

    @Override
    public void activateHotel(Long hotelID) {
        log.info("Activationg Hotel details by ID {}", hotelID);
        Hotel hotel = hotelRepo.findById(hotelID).orElseThrow(()-> new ResourceNotFoundException("Hotel not found with this ID" +hotelID));

        hotel.setActive(true);
        hotelRepo.save(hotel);

//       TODO -> Inventory update ->
    }

    @Override
    public List<HotelDTO> getAllHotels() {
        log.info("Fetching all hotels");

        List<Hotel> hotels = hotelRepo.findAll();

        return hotels.stream()
                .map(hotel -> modelMapper.map(hotel, HotelDTO.class))
                .collect(Collectors.toList());
    }


}
