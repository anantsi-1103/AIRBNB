package com.logic.strategy;

import com.logic.entity.Inventory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.MonthDay;
import java.util.Set;

@RequiredArgsConstructor
public class HolidayPricingStrategy implements PricingStrategy {

    private final PricingStrategy wrapped;
    private static final Set<MonthDay> HOLIDAYS = Set.of(
            MonthDay.of(1, 1),
            MonthDay.of(1, 26),
            MonthDay.of(8, 15),
            MonthDay.of(10, 2),
            MonthDay.of(12, 25)
    );


    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);

        boolean isHoliday = HOLIDAYS.contains(MonthDay.from(inventory.getDate()));
        boolean isWeekend = inventory.getDate().getDayOfWeek() == DayOfWeek.SATURDAY
                || inventory.getDate().getDayOfWeek() == DayOfWeek.SUNDAY;

        if(isHoliday || isWeekend){
            price = price.multiply(BigDecimal.valueOf(1.25));
        }

        return price;

    }
}
