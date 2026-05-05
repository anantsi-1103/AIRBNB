package com.logic.Service;

import com.logic.DTO.*;
import com.logic.Repository.*;
import com.logic.entity.*;
import com.logic.entity.enums.BookingStatus;
import com.logic.exception.ResourceNotFoundException;
import com.logic.exception.UnAuthorisedException;
import com.logic.strategy.PricingService;
import com.razorpay.RazorpayClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Service
public class BookServiceImpl implements BookingService{

    private final ModelMapper modelMapper;
    private final GuestRepository guestRepository;

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final InventoryRepository inventoryRepository;
    private final PricingService pricingService;
    private final CheckoutService checkoutService;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Override
    @Transactional
    // Atomicity ->
    public BookingDTO intializeBooking(BookingRequest bookingRequest) {
      log.info("Intializing bookinf for hotel : {} , room : {}, date : {} - {}",
              bookingRequest.getHotelId(), bookingRequest.getRoomId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());

      if (bookingRequest.getHotelId() == null || bookingRequest.getRoomId() == null) {
          throw new IllegalArgumentException("Hotel ID and room ID are required");
      }

      if (bookingRequest.getRoomsCount() == null || bookingRequest.getRoomsCount() <= 0) {
          throw new IllegalArgumentException("Rooms count must be greater than zero");
      }

      if (bookingRequest.getCheckInDate() == null || bookingRequest.getCheckOutDate() == null) {
          throw new IllegalArgumentException("Check-in date and check-out date are required");
      }

      Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
              .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID : " + bookingRequest.getHotelId()));

      Room room = roomRepository.findById(bookingRequest.getRoomId())
              .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID : " + bookingRequest.getRoomId()));

      if (!room.getHotel().getId().equals(hotel.getId())) {
          throw new IllegalArgumentException("Room does not belong to the selected hotel");
      }

      if (!Boolean.TRUE.equals(hotel.getActive())) {
          throw new IllegalStateException("Hotel is not active");
      }

      long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());

      if (daysCount <= 0) {
          throw new IllegalArgumentException("Check-out date must be after check-in date");
      }

      List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(bookingRequest.getRoomId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(),
              bookingRequest.getRoomsCount());

        if(inventoryList.size() != daysCount){
            throw new IllegalStateException("Room is not available anymore");
        }

        inventoryList.forEach(inventory ->
                inventory.setPrice(pricingService.calculateDynamicPricing(inventory)));

        for(Inventory inventory : inventoryList){
            // 0 + 5 = 15
            inventory.setReservedCount(inventory.getReservedCount()+ bookingRequest.getRoomsCount());
        }

        // dynamic prices 
        inventoryRepository.saveAll(inventoryList);

//        Booking -> reserve krna

        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequest.getRoomsCount())
                .guests(new HashSet<>())
                .build();
        // Calculate actual amount
        booking.setAmount(
                calculateBookingAmount(inventoryList, bookingRequest.getRoomsCount())
        );

        log.info("Calculated booking amount: {}", booking.getAmount());

        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDTO.class);

    }

    @Override
    @Transactional
    public BookingDTO addGuests(Long bookingId, List<GuestDTO> guestDtoList) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with id: " + bookingId));

        if (hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking has already expired");
        }

        if (booking.getBookingStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException(
                    "Booking is not under reserved state");
        }

        for (GuestDTO guestDto : guestDtoList) {

            Guest guest = modelMapper.map(guestDto, Guest.class);

            guest.setUser(getCurrentUser());

            guestRepository.save(guest);

            if (booking.getGuests() == null) {
                booking.setGuests(new HashSet<>());
            }

            booking.getGuests().add(guest);
        }

        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);

        bookingRepository.save(booking);

        return modelMapper.map(booking, BookingDTO.class);
    }


    @Override
    @Transactional
    public BookingPaymentInitResponseDTO initiatePayments(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );


        User user = getCurrentUser(); // authenticate
        if (!user.getId().equals(booking.getUser().getId())) {
            throw new UnAuthorisedException(
                    "Booking does not belong to this user with id: " + user.getId()
            );
        }
        if (hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking has already expired");
        }

        BookingPaymentInitResponseDTO paymentOrder = checkoutService.createPaymentOrder(booking);

        booking.setBookingStatus(BookingStatus.PAYMENTS_PENDING);
        bookingRepository.save(booking);

        return paymentOrder;
    }

    @Override
    @Transactional
    public BookingDTO verifyPayment(Long bookingId, BookingPaymentVerifyRequestDTO paymentVerifyRequest) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );
        User user = getCurrentUser();
        if (!user.getId().equals(booking.getUser().getId())) {
            throw new UnAuthorisedException(
                    "Booking does not belong to this user with id: " + user.getId()
            );
        }
        if (hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking has already expired");
        }
        if (booking.getRazorpayOrderId() == null ||
                !booking.getRazorpayOrderId().equals(paymentVerifyRequest.getRazorpayOrderId())) {
            throw new IllegalStateException("Payment order does not belong to this booking");
        }
        if (!isValidRazorpaySignature(
                paymentVerifyRequest.getRazorpayOrderId(),
                paymentVerifyRequest.getRazorpayPaymentId(),
                paymentVerifyRequest.getRazorpaySignature()
        )) {
            throw new IllegalStateException("Razorpay payment signature verification failed");
        }

        confirmPaidBooking(booking, paymentVerifyRequest.getRazorpayPaymentId());
        return modelMapper.map(booking, BookingDTO.class);
    }

    private void confirmPaidBooking(Booking booking, String razorpayPaymentId) {
        booking.setRazorpayPaymentId(razorpayPaymentId);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());

        inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());

        log.info("Successfully confirmed the booking for Booking ID: {}", booking.getId());
    }

    private boolean isValidRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String expectedSignature = hmacSha256(orderId + "|" + paymentId, razorpayKeySecret);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            throw new RuntimeException("Could not verify Razorpay payment signature", e);
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

    public boolean hasBookingExpired(Booking booking){
        return booking.getCreatedAt().plusMinutes(10).isBefore((LocalDateTime.now()));
    }






    public User getCurrentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("Authenticated user not found");
        }

        return user;
    }

    private BigDecimal calculateBookingAmount(List<Inventory> inventoryList, Integer roomsCount) {
        return inventoryList.stream()
                .map(Inventory::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(roomsCount));
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

}
