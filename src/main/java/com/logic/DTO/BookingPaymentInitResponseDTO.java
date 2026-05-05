package com.logic.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingPaymentInitResponseDTO {

    private String key;
    private String orderId;
    private Long bookingId;
    private Long amount;
    private String currency;
    private String name;
    private String description;
}
