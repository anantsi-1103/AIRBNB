package com.logic.Service;

import com.logic.DTO.BookingDTO;
import com.logic.DTO.InvoiceDTO;
import com.logic.DTO.PaymentDTO;
import com.logic.DTO.PaymentFailureRequestDTO;
import com.logic.Repository.BookingRepository;
import com.logic.Repository.InventoryRepository;
import com.logic.Repository.PaymentRepository;
import com.logic.entity.Booking;
import com.logic.entity.Inventory;
import com.logic.entity.Payment;
import com.logic.entity.User;
import com.logic.entity.enums.BookingStatus;
import com.logic.entity.enums.PaymentStatus;
import com.logic.exception.ResourceNotFoundException;
import com.logic.exception.UnAuthorisedException;
import com.logic.strategy.PricingService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final InventoryRepository inventoryRepository;
    private final PricingService pricingService;
    private final RazorpayClient razorpayClient;
    private final ModelMapper modelMapper;

    @Value("${razorpay.currency:INR}")
    private String currency;

    @Value("${razorpay.webhook.secret:replace_me}")
    private String webhookSecret;

    public List<PaymentDTO> getCurrentUserPayments() {
        User user = getCurrentUser();
        return paymentRepository.findByBookingUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toPaymentDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentDTO recordFailedPayment(Long bookingId, PaymentFailureRequestDTO failureRequest) {
        Booking booking = getOwnedBooking(bookingId);

        Payment payment = paymentRepository.findByRazorpayOrderId(failureRequest.getRazorpayOrderId())
                .orElseGet(() -> Payment.builder()
                        .booking(booking)
                        .amount(booking.getAmount())
                        .currency(currency)
                        .razorpayOrderId(failureRequest.getRazorpayOrderId())
                        .build());

        payment.setBooking(booking);
        payment.setRazorpayPaymentId(failureRequest.getRazorpayPaymentId());
        payment.setFailureReason(buildFailureReason(failureRequest));
        payment.setPaymentStatus(PaymentStatus.FAILED);

        return toPaymentDTO(paymentRepository.save(payment));
    }

    @Transactional
    public BookingDTO cancelOrRefundBooking(Long bookingId) {
        Booking booking = getOwnedBooking(bookingId);

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            return modelMapper.map(booking, BookingDTO.class);
        }

        Payment payment = booking.getRazorpayOrderId() == null
                ? null
                : paymentRepository.findByRazorpayOrderId(booking.getRazorpayOrderId()).orElse(null);

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            if (payment == null || payment.getRazorpayPaymentId() == null) {
                throw new IllegalStateException("Cannot refund booking without a successful payment record");
            }

            refundPayment(payment);
            applyInventoryCountChanges(booking, 0, -booking.getRoomsCount());
        } else {
            releaseReservedInventoryIfPresent(booking);
            if (payment != null && payment.getPaymentStatus() != PaymentStatus.FAILED) {
                payment.setPaymentStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(payment);
            }
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDTO.class);
    }

    public InvoiceDTO buildInvoice(Long bookingId) {
        Booking booking = getOwnedBooking(bookingId);

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Invoice is available only after confirmed payment");
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(booking.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking id: " + bookingId));

        String content = """
                Air Stay Receipt
                =================
                Booking ID: %s
                Payment ID: %s
                Order ID: %s
                Hotel: %s
                Room: %s
                Check-in: %s
                Check-out: %s
                Rooms: %s
                Amount: %s %s
                Payment Status: %s
                Paid At: %s
                """.formatted(
                booking.getId(),
                nullToDash(payment.getRazorpayPaymentId()),
                nullToDash(payment.getRazorpayOrderId()),
                booking.getHotel().getName(),
                booking.getRoom().getType(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomsCount(),
                payment.getCurrency(),
                payment.getAmount(),
                payment.getPaymentStatus(),
                payment.getUpdatedAt() == null ? "-" : payment.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        return new InvoiceDTO("booking-" + booking.getId() + "-receipt.txt", content.getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public void handleRazorpayWebhook(String payload, String signature) {
        if (!isValidSignature(payload, signature, webhookSecret)) {
            throw new UnAuthorisedException("Invalid Razorpay webhook signature");
        }

        JSONObject body = new JSONObject(payload);
        String event = body.optString("event");
        JSONObject eventPayload = body.optJSONObject("payload");
        if (eventPayload == null) {
            return;
        }

        JSONObject payloadType = eventPayload.optJSONObject(event.startsWith("refund.") ? "refund" : "payment");
        if (payloadType == null) {
            return;
        }

        JSONObject entity = payloadType.optJSONObject("entity");

        if (entity == null) {
            return;
        }

        switch (event) {
            case "payment.captured", "payment.authorized" -> confirmPaymentFromWebhook(
                    entity.optString("order_id"),
                    entity.optString("id")
            );
            case "payment.failed" -> recordFailedPaymentFromWebhook(entity);
            case "refund.processed" -> recordRefundFromWebhook(entity);
            default -> {
            }
        }
    }

    @Transactional
    public void confirmPaymentFromWebhook(String razorpayOrderId, String razorpayPaymentId) {
        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
            return;
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for Razorpay order id: " + razorpayOrderId));
        Booking booking = payment.getBooking();

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
        paymentRepository.save(payment);

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            booking.setRazorpayPaymentId(razorpayPaymentId);
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            applyInventoryCountChanges(booking, -booking.getRoomsCount(), booking.getRoomsCount());
            bookingRepository.save(booking);
        }
    }

    private void recordFailedPaymentFromWebhook(JSONObject entity) {
        String orderId = entity.optString("order_id");
        if (orderId == null || orderId.isBlank()) {
            return;
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for Razorpay order id: " + orderId));

        payment.setRazorpayPaymentId(entity.optString("id", null));
        payment.setFailureReason(entity.optString("error_description", entity.optString("error_reason", "Payment failed")));
        payment.setPaymentStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
    }

    private void recordRefundFromWebhook(JSONObject entity) {
        String paymentId = entity.optString("payment_id");
        paymentRepository.findByRazorpayPaymentId(paymentId).ifPresent(payment -> {
            payment.setRazorpayRefundId(entity.optString("id", null));
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        });
    }

    private void refundPayment(Payment payment) {
        try {
            com.razorpay.Payment razorpayPayment = razorpayClient.payments.fetch(payment.getRazorpayPaymentId());
            String razorpayStatus = getRazorpayString(razorpayPayment, "status");
            log.info("Fetched Razorpay payment before refund. paymentId={}, orderId={}, status={}, amount={}, refundStatus={}",
                    payment.getRazorpayPaymentId(),
                    getRazorpayString(razorpayPayment, "order_id"),
                    razorpayStatus,
                    razorpayPayment.toJson().opt("amount"),
                    getRazorpayString(razorpayPayment, "refund_status"));

            if (!"captured".equalsIgnoreCase(razorpayStatus) && !"authorized".equalsIgnoreCase(razorpayStatus)) {
                throw new IllegalStateException("Cannot refund Razorpay payment because its status is: " + razorpayStatus);
            }

            if ("authorized".equalsIgnoreCase(razorpayStatus)) {
                JSONObject captureRequest = new JSONObject();
                captureRequest.put("amount", getAmountInPaise(payment));
                captureRequest.put("currency", payment.getCurrency());
                razorpayClient.payments.capture(payment.getRazorpayPaymentId(), captureRequest);
            }

            Refund refund = createRefund(payment);

            payment.setRazorpayRefundId(getRazorpayString(refund, "id"));
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        } catch (RazorpayException e) {
            throw new IllegalStateException("Could not refund Razorpay payment: " + e.getMessage(), e);
        }
    }

    private Refund createRefund(Payment payment) throws RazorpayException {
        try {
            return razorpayClient.payments.refund(payment.getRazorpayPaymentId());
        } catch (RazorpayException fullRefundException) {
            log.warn("Full Razorpay refund without amount failed for payment {}, retrying with explicit amount",
                    payment.getRazorpayPaymentId(), fullRefundException);

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", getAmountInPaise(payment));
            refundRequest.put("speed", "normal");
            return razorpayClient.payments.refund(payment.getRazorpayPaymentId(), refundRequest);
        }
    }

    private long getAmountInPaise(Payment payment) {
        return payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String getRazorpayString(com.razorpay.Entity entity, String key) {
        Object value = entity.toJson().opt(key);
        return value == null ? null : String.valueOf(value);
    }

    private Booking getOwnedBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        User user = getCurrentUser();

        if (!user.getId().equals(booking.getUser().getId())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id: " + user.getId());
        }

        return booking;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("Authenticated user not found");
        }

        return user;
    }

    private void releaseReservedInventoryIfPresent(Booking booking) {
        if (booking.getBookingStatus() == BookingStatus.RESERVED ||
                booking.getBookingStatus() == BookingStatus.GUESTS_ADDED ||
                booking.getBookingStatus() == BookingStatus.PAYMENTS_PENDING) {
            applyInventoryCountChanges(booking, -booking.getRoomsCount(), 0);
        }
    }

    private void applyInventoryCountChanges(Booking booking, int reservedDelta, int bookedDelta) {
        List<Inventory> inventoryList = getLockedBookingInventory(booking);

        for (Inventory inventory : inventoryList) {
            int nextReservedCount = inventory.getReservedCount() + reservedDelta;
            int nextBookedCount = inventory.getBookedCount() + bookedDelta;

            if (nextReservedCount < 0 || nextBookedCount < 0 || nextReservedCount + nextBookedCount > inventory.getTotalCount()) {
                throw new IllegalStateException("Invalid inventory count update for booking id: " + booking.getId());
            }

            inventory.setReservedCount(nextReservedCount);
            inventory.setBookedCount(nextBookedCount);
            inventory.setPrice(pricingService.calculateDynamicPricing(inventory));
        }

        inventoryRepository.saveAll(inventoryList);
    }

    private List<Inventory> getLockedBookingInventory(Booking booking) {
        List<Inventory> inventoryList = inventoryRepository.findAndLockInventoryByRoomAndDateRange(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );

        long daysCount = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        if (inventoryList.size() != daysCount) {
            throw new IllegalStateException("Inventory records are missing for booking id: " + booking.getId());
        }

        return inventoryList;
    }

    private boolean isValidSignature(String payload, String signature, String secret) {
        if (signature == null || signature.isBlank() || secret == null || secret.isBlank() || "replace_me".equals(secret)) {
            return false;
        }

        try {
            String expectedSignature = hmacSha256(payload, secret);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            throw new RuntimeException("Could not verify Razorpay webhook signature", e);
        }
    }

    private String hmacSha256(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(hash.length * 2);

        for (byte currentByte : hash) {
            hex.append(String.format("%02x", currentByte));
        }

        return hex.toString();
    }

    private String buildFailureReason(PaymentFailureRequestDTO failureRequest) {
        return String.join(" | ",
                nullToDash(failureRequest.getCode()),
                nullToDash(failureRequest.getDescription()),
                nullToDash(failureRequest.getSource()),
                nullToDash(failureRequest.getStep()),
                nullToDash(failureRequest.getReason())
        );
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private PaymentDTO toPaymentDTO(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .razorpayRefundId(payment.getRazorpayRefundId())
                .failureReason(payment.getFailureReason())
                .paymentStatus(payment.getPaymentStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
