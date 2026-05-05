package com.logic.Service;

import com.logic.DTO.BookingPaymentInitResponseDTO;
import com.logic.entity.Booking;

public interface CheckoutService {

    BookingPaymentInitResponseDTO createPaymentOrder(Booking booking);
}
