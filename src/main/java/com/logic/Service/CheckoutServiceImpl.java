package com.logic.Service;

import com.logic.DTO.BookingPaymentInitResponseDTO;
import com.logic.Repository.BookingRepository;
import com.logic.Repository.PaymentRepository;
import com.logic.entity.Booking;
import com.logic.entity.Payment;
import com.logic.entity.enums.PaymentStatus;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.currency:INR}")
    private String currency;

    @Value("${razorpay.business.name:Air Stay}")
    private String businessName;

    @Override
    public BookingPaymentInitResponseDTO createPaymentOrder(Booking booking) {
        log.info("Creating Razorpay order for booking with ID: {}", booking.getId());

        try {

                // to string
            log.info("Booking: {}", booking);
            log.info("Amount: {}", booking.getAmount());


            Long amountInPaise = booking.getAmount().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
            String description = booking.getHotel().getName() + " : " + booking.getRoom().getType();

            JSONObject notes = new JSONObject();
            notes.put("bookingId", booking.getId());
            notes.put("hotelName", booking.getHotel().getName());
            notes.put("roomType", booking.getRoom().getType());

            log.info("Amount in paise: {}", amountInPaise);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "booking_" + booking.getId());
            orderRequest.put("notes", notes);

            log.info("Order request: {}", orderRequest);

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = String.valueOf(order.toJson().opt("id"));
            log.info("Order created: {}", order);

            booking.setRazorpayOrderId(orderId);
            bookingRepository.save(booking);
            paymentRepository.save(Payment.builder()
                    .booking(booking)
                    .amount(booking.getAmount())
                    .currency(currency)
                    .razorpayOrderId(orderId)
                    .paymentStatus(PaymentStatus.PENDING)
                    .build());

            log.info("Razorpay order created for booking ID: {}", booking.getId());
            return new BookingPaymentInitResponseDTO(razorpayKeyId, orderId, booking.getId(), amountInPaise, currency, businessName, description);
        } catch (RazorpayException e) {
            throw new RuntimeException("Could not create Razorpay order", e);
        }
    }


}
