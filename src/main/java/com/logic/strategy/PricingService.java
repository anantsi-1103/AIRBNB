package com.logic.strategy;

import com.logic.entity.Inventory;

import java.math.BigDecimal;

public class PricingService {



    public BigDecimal calculateDynamicPricing(Inventory inventory){
        PricingStrategy pricingStrategy = new BasePricingStrategy();
        // apply all the additional strategy
        pricingStrategy = new OccupancyPricing(pricingStrategy);
        pricingStrategy = new UrgencyPricingStrategy(pricingStrategy);
        pricingStrategy = new HolidayPricingStrategy(pricingStrategy);

        return pricingStrategy.calculatePrice(inventory);
    }
}
