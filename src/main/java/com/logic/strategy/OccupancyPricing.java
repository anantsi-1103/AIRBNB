package com.logic.strategy;

import com.logic.entity.Inventory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class OccupancyPricing implements PricingStrategy {

    private final PricingStrategy wrapped;


    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);

        // 80 / 100 -> 0.
        double occupancyRate = (double)inventory.getBookedCount()/ inventory.getTotalCount();

        if(occupancyRate > 0.8){
            price = price.multiply(BigDecimal.valueOf(1.2));
        }
        return price;

    }
}
