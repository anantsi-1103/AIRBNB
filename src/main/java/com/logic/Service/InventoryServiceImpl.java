package com.logic.Service;

import com.logic.DTO.HotelDTO;
import com.logic.DTO.HotelSearchRequest;
import com.logic.DTO.InventoryUpdateRequest;
import com.logic.Repository.InventoryRepository;
import com.logic.entity.Hotel;
import com.logic.entity.Inventory;
import com.logic.entity.Room;
import com.logic.strategy.PricingService;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;
    private final PricingService pricingService;

    @Override
    @Transactional
    public void initializeRoomForAYear(Room room) {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);

        List<Inventory> inventories = new ArrayList<>();
        Set<LocalDate> existingDates = inventoryRepository
                .findByRoomAndDateBetween(room, startDate, endDate)
                .stream()
                .map(Inventory::getDate)
                .collect(Collectors.toSet());

        LocalDate currentDate = startDate;

        while (currentDate.isBefore(endDate)) {
            if (existingDates.contains(currentDate)) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .reservedCount(0)
                    .city(room.getHotel().getCity())
                    .date(currentDate)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            inventory.setPrice(pricingService.calculateDynamicPricing(inventory));

            inventories.add(inventory);

            currentDate = currentDate.plusDays(1);
        }

        inventoryRepository.saveAll(inventories);

        log.info("Initialized {} inventory records for Room ID {}",
                inventories.size(),
                room.getId());
    }

    @Override
    public void deleteAllInventories(Room room) {
        log.info("Deleting the inventories of room with id: {}", room.getId());
        inventoryRepository.deleteByRoom(room);
    }
    @Override
    public Page<HotelDTO> searchHotels(HotelSearchRequest hotelSearchRequest) {
       log.info("Searching Hotels for {} city , from {} to {}", hotelSearchRequest.getCity(),hotelSearchRequest.getStartDate(),hotelSearchRequest.getEndDate());

        if (hotelSearchRequest.getCity() == null || hotelSearchRequest.getCity().isBlank()) {
            throw new IllegalArgumentException("City is required");
        }

        if (hotelSearchRequest.getRoomsCount() == null || hotelSearchRequest.getRoomsCount() <= 0) {
            throw new IllegalArgumentException("Rooms count must be greater than zero");
        }

        if (hotelSearchRequest.getStartDate() == null || hotelSearchRequest.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        Pageable pageable = PageRequest.of(hotelSearchRequest.getPage(), hotelSearchRequest.getSize());
        long dateCount = ChronoUnit.DAYS.between(hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate());

        if (dateCount <= 0) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // business logic - 90 days
        Page<Hotel> hotelPage =
                inventoryRepository.findHotelsWithAvailableInventory(hotelSearchRequest.getCity(),
                        hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate(), hotelSearchRequest.getRoomsCount(),
                        dateCount, pageable);

//        mapping
        return hotelPage.map((element)-> modelMapper.map(element, HotelDTO.class));

    }

    @Override
    @Transactional
    public void updateRoomInventory(Long roomId, InventoryUpdateRequest inventoryUpdateRequest) {
        if (inventoryUpdateRequest.getStartDate() == null || inventoryUpdateRequest.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        if (!inventoryUpdateRequest.getEndDate().isAfter(inventoryUpdateRequest.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (inventoryUpdateRequest.getSurgeFactor() != null
                && inventoryUpdateRequest.getSurgeFactor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Surge factor must be greater than zero");
        }

        List<Inventory> inventories = inventoryRepository.findAndLockInventoryByRoomAndDateRange(
                roomId,
                inventoryUpdateRequest.getStartDate(),
                inventoryUpdateRequest.getEndDate()
        );

        long dateCount = ChronoUnit.DAYS.between(
                inventoryUpdateRequest.getStartDate(),
                inventoryUpdateRequest.getEndDate()
        );

        if (inventories.size() != dateCount) {
            throw new IllegalStateException("Inventory records are missing for the selected date range");
        }

        for (Inventory inventory : inventories) {
            if (inventoryUpdateRequest.getSurgeFactor() != null) {
                inventory.setSurgeFactor(inventoryUpdateRequest.getSurgeFactor());
            }

            if (inventoryUpdateRequest.getClosed() != null) {
                inventory.setClosed(inventoryUpdateRequest.getClosed());
            }

            inventory.setPrice(pricingService.calculateDynamicPricing(inventory));
        }

        inventoryRepository.saveAll(inventories);
    }
}
