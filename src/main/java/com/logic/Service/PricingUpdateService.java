package com.logic.Service;

import com.logic.Repository.HotelMinPriceRepository;
import com.logic.Repository.HotelRepository;
import com.logic.Repository.InventoryRepository;
import com.logic.entity.Hotel;
import com.logic.entity.Inventory;
import com.logic.strategy.PricingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PricingUpdateService {

    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;
    private final HotelMinPriceRepository hotelMinPriceRepository;
    private final PricingService pricingService;

    
    
    @Scheduled(cron="0 0 * * * *")
    public void updatePrices(){
        int page = 0;
        int batchSize = 100;
        
        while(true){
            Page<Hotel> hotelpage = hotelRepository.findAll(PageRequest.of(page,batchSize));
            
            if(hotelpage.isEmpty()){
                log.info("Hotel not found");
                break;
            }
            
//            
            hotelpage.getContent().forEach(this::UpdateHotelPrices);
            
            page++;
            
            
        }
        
    }

    private void UpdateHotelPrices(Hotel hotel) {
        log.info("Updating the hotel prices for hotel id : {}", hotel.getId());

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusYears(1);
        
        List<Inventory> inventoryList = inventoryRepository.findByHotelAndDateBetween(hotel, startDate, endDate);
        
        updateInventoryPrices(inventoryList);
        
        updateHotelMinPrices(hotel, inventoryList, startDate , endDate);
    }

    private void updateHotelMinPrices(Hotel hotel, List<Inventory> inventoryList, LocalDate startDate, LocalDate endDate) {
    }

    private void updateInventoryPrices(List<Inventory> inventoryList) {
        inventoryList.forEach(inventory -> {
            BigDecimal dynamicPrice = pricingService.calculateDynamicPricing(inventory);
            inventory.setPrice(dynamicPrice);
        });
        
        inventoryRepository.saveAll(inventoryList);
    }


}
