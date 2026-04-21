package com.logic.Service;

import com.logic.DTO.HotelDTO;
import com.logic.DTO.HotelSearchRequest;
import com.logic.Repository.InventoryRepository;
import com.logic.entity.Hotel;
import com.logic.entity.Inventory;
import com.logic.entity.Room;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public void initializeRoomForAYear(Room room) {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);

        List<Inventory> inventories = new ArrayList<>();

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {

            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .city(room.getHotel().getCity())
                    .date(currentDate)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();

            inventories.add(inventory);

            currentDate = currentDate.plusDays(1);
        }

        inventoryRepository.saveAll(inventories);

        log.info("Initialized {} inventory records for Room ID {}",
                inventories.size(),
                room.getId());
    }

    @Override
    @Transactional
    public void deleteFutureInventories(Room room) {

        LocalDate today = LocalDate.now();

        int deletedCount =
                inventoryRepository.deleteByDateAfterAndRoom(today, room);

        log.info("Deleted {} future inventories for Room ID {}",
                deletedCount,
                room.getId());
    }

    @Override
    public Page<HotelDTO> searchHotels(HotelSearchRequest hotelSearchRequest) {
       log.info("Searching Hotels for {} city , from {} to {}", hotelSearchRequest.getCity(),hotelSearchRequest.getStartDate(),hotelSearchRequest.getEndDate());

        Pageable pageable = PageRequest.of(hotelSearchRequest.getPage(), hotelSearchRequest.getSize());
        long dateCount = ChronoUnit.DAYS.between(hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate());

        Page<Hotel> hotelPage = inventoryRepository.findHotelsWithAvailableInventory
                (hotelSearchRequest.getCity(),hotelSearchRequest.getStartDate(),
                        hotelSearchRequest.getEndDate(),hotelSearchRequest.getRoomsCount(),
                        dateCount,pageable);

//        mapping
        return hotelPage.map((element)-> modelMapper.map(element, HotelDTO.class));

    }
}