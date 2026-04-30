package com.logic.Service;

import com.logic.Repository.HotelMinPriceRepository;
import com.logic.Repository.HotelRepository;
import com.logic.Repository.InventoryRepository;
import com.logic.entity.Hotel;
import com.logic.entity.HotelMinPrice;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<LocalDate, BigDecimal> dailyMinPrice = inventoryList.stream()
                .collect(Collectors.groupingBy(
                        Inventory::getDate,
                        //get the price on the date -> only the min price
                        Collectors.mapping(Inventory::getPrice, Collectors.minBy(Comparator.naturalOrder()))
                ))
                .entrySet().stream()
                // key is the date -> value -> or else 0
                .collect(Collectors.toMap(Map.Entry::getKey, e-> e.getValue().orElse(BigDecimal.ZERO)));

        // date ke sath price ko group krdiya
        // prepare hotelprices entites in bulk

        List<HotelMinPrice> hotelMinPrices = new ArrayList<>();

        dailyMinPrice.forEach((date, price) -> {

            // find existing record by hotel and date
            HotelMinPrice hotelPrice =
                    hotelMinPriceRepository
                            .findByHotelAndDate(hotel, date)

                            // if not found create new
                            .orElse(new HotelMinPrice(hotel, date));

            // set price
            hotelPrice.setPrice(price);

            // add to list
            hotelMinPrices.add(hotelPrice);

        });

// save all records
        hotelMinPriceRepository.saveAll(hotelMinPrices);

    }


    private void updateInventoryPrices(List<Inventory> inventoryList) {
        inventoryList.forEach(inventory -> {
            BigDecimal dynamicPrice = pricingService.calculateDynamicPricing(inventory);
            inventory.setPrice(dynamicPrice);
        });
        inventoryRepository.saveAll(inventoryList);

    }


}
