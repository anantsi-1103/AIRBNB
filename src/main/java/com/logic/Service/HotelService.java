package com.logic.Service;

import com.logic.DTO.HotelDTO;
import org.springframework.stereotype.Service;


public interface HotelService {
    HotelDTO createNewHotel (HotelDTO hotelDTO);
}
