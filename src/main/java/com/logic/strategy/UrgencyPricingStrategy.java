package com.logic.strategy;

import com.logic.entity.Inventory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@RequiredArgsConstructor
public class UrgencyPricingStrategy implements PricingStrategy {

    private final PricingStrategy wrapped;


    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);

        // 80 / 100 -> 0.
        LocalDate today = LocalDate.now();

//     today se zada or 7 se din se km
        if((!inventory.getDate().isBefore(today)) && (inventory.getDate().isBefore(today.plusDays(7)))){
            price = price.multiply(BigDecimal.valueOf(1.15));
        }

        return price;

    }
}
