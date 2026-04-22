package com.logic.Service;

import com.logic.DTO.HotelDTO;
import com.logic.DTO.HotelSearchRequest;
import com.logic.entity.Room;
import org.springframework.data.domain.Page;

public interface InventoryService {

    void initializeRoomForAYear(Room room);

    void deleteAllInventories(Room room);

    Page<HotelDTO> searchHotels(HotelSearchRequest hotelSearchRequest);
}
