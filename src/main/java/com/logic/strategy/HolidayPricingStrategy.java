package com.logic.strategy;

import com.logic.entity.Inventory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@RequiredArgsConstructor
public class HolidayPricingStrategy implements PricingStrategy {

    private final PricingStrategy wrapped;


    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);

        // API ->
        boolean isTodayHoliday = true; //
        if(isTodayHoliday){
            price = price.multiply(BigDecimal.valueOf(1.25));
        }

        return price;

    }
}
