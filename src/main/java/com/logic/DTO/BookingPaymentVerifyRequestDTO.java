package com.logic.DTO;

import lombok.Data;

@Data
public class BookingPaymentVerifyRequestDTO {
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String razorpaySignature;
}
