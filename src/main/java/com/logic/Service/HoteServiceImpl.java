package com.logic.Service;

import com.logic.DTO.HotelDTO;
import com.logic.Repository.HotelRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class HoteServiceImpl implements HotelService{

    private HotelRepo hotelRepo;
    private ModelMapper modelMapper;

    @Override
    public HotelDTO createNewHotel(HotelDTO hotelDTO) {
        log.info("Creating a new Hotel with name : {}", hotelDTO.getName());

    }
}
