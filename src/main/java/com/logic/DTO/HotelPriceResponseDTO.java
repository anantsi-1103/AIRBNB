package com.logic.DTO;

import com.logic.entity.HotelContactInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelPriceResponseDTO {
    private Long id;
    private String name;
    private String city;
    private List<String> photos;
    private List<String> amenities;
    private HotelContactInfo contactInfo;
    private Double price;

    public HotelPriceResponseDTO(Long id,
                                 String name,
                                 String city,
                                 List<String> photos,
                                 List<String> amenities,
                                 HotelContactInfo contactInfo,
                                 BigDecimal price) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.photos = photos;
        this.amenities = amenities;
        this.contactInfo = contactInfo;
        this.price = price == null ? null : price.doubleValue();
    }
}
