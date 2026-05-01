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

        if (inventory.getTotalCount() == null || inventory.getTotalCount() <= 0) {
            return price;
        }

        int bookedCount = inventory.getBookedCount() == null ? 0 : inventory.getBookedCount();
        int reservedCount = inventory.getReservedCount() == null ? 0 : inventory.getReservedCount();
        double occupancyRate = (double) (bookedCount + reservedCount) / inventory.getTotalCount();

        if(occupancyRate > 0.8){
            price = price.multiply(BigDecimal.valueOf(1.2));
        }
        return price;

    }
}
