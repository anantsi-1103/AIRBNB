package com.logic.DTO;

import com.logic.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomPriceDTO {
    private Room room;
    private Double price;
}
