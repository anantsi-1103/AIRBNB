package com.logic.DTO;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HotelInfoRequestDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long roomsCount;
}
