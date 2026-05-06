package com.logic.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomPriceResponseDTO {
    private Long id;
    private String type;
    private String[] photos;
    private String[] amenities;
    private Double price;
}
