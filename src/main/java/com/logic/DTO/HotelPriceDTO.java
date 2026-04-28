package com.logic.DTO;

import com.logic.entity.Hotel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelPriceDTO {

    private Hotel hotel;
    private Double price;
}
