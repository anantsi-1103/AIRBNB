package com.logic.DTO;

import lombok.Data;

@Data
public class PaymentFailureRequestDTO {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String code;
    private String description;
    private String source;
    private String step;
    private String reason;
}
