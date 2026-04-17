package com.logic.Service;

import com.logic.DTO.HotelDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface HotelService {
    HotelDTO createNewHotel (HotelDTO hotelDTO);

    HotelDTO getHotelByID(Long id);

    HotelDTO updateHotelById(Long id, HotelDTO hotelDTO);

    void deleteHotelById(Long id);

    void activateHotel(Long hotelID);

    List<HotelDTO> getAllHotels();
}
