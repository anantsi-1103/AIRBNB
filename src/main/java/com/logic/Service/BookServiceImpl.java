package com.logic.Service;

import com.logic.DTO.BookingDTO;
import com.logic.DTO.BookingRequest;
import com.logic.DTO.GuestDTO;
import com.logic.Repository.GuestRepository;
import com.logic.Repository.HotelRepository;
import com.logic.Repository.InventoryRepository;
import com.logic.Repository.RoomRepository;
import com.logic.entity.Hotel;
import com.logic.entity.Inventory;
import com.logic.entity.Room;
import com.logic.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Service
public class BookServiceImpl implements BookingService{

    private final ModelMapper modelMapper;
    private final GuestRepository guestRepository;

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    // Atomicity ->
    public BookingDTO intializeBooking(BookingRequest bookingRequest) {
      log.info("Intializing bookinf for hotel : {} , room : {}, date : {} - {}",
              bookingRequest.getHotelId(), bookingRequest.getRoomId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());

      Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
              .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID : " + bookingRequest.getHotelId()));

      Room room = roomRepository.findById(bookingRequest.getRoomId())
              .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID : " + bookingRequest.getRoomId()));

      List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(bookingRequest.getRoomId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(),
              bookingRequest.getRoomsCount());

        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate())+ 1;

        if(inventoryList.size() != daysCount){
            throw new IllegalStateException("Room is not available anymore");
        }

        for(Inventory inventory : inventoryList){
            // 0 + 5 = 15
            inventory.setReservedCount(inventory.getReservedCount()+ bookingRequest.getRoomsCount());
        }

        // dynamic prices 
        inventoryRepository.saveAll(inventoryList);

//        Booking -> reserve krna
    }

    @Override
    public BookingDTO addGuest(Long bookindId, List<GuestDTO> guestDTOList) {
        return null;
    }
}
