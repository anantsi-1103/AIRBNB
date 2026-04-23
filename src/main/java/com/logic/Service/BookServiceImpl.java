package com.logic.Service;

import com.logic.DTO.BookingDTO;
import com.logic.DTO.BookingRequest;
import com.logic.DTO.GuestDTO;
import com.logic.Repository.*;
import com.logic.entity.*;
import com.logic.entity.enums.BookingStatus;
import com.logic.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    @Override
    @Transactional
    // Atomicity ->
    public BookingDTO intializeBooking(BookingRequest bookingRequest) {
      log.info("Intializing bookinf for hotel : {} , room : {}, date : {} - {}",
              bookingRequest.getHotelId(), bookingRequest.getRoomId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());

      Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
              .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID : " + bookingRequest.getHotelId()));

      Room room = roomRepository.findById(bookingRequest.getRoomId())
              .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID : " + bookingRequest.getRoomId()));

      List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(bookingRequest.getRoomId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(),
              bookingRequest.getRoomsCount());

        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate())+ 1;

        if(inventoryList.size() != daysCount){
            throw new IllegalStateException("Room is not available anymore");
        }

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
                .build();
        // Calculate actual amount
        booking.setAmount(
                calculateBookingAmount(booking)
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

            booking.getGuests().add(guest);
        }

        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);

        bookingRepository.save(booking);

        return modelMapper.map(booking, BookingDTO.class);
    }

    public boolean hasBookingExpired(Booking booking){
        return booking.getCreatedAt().plusMinutes(10).isBefore((LocalDateTime.now()));
    }






    public User getCurrentUser(){
        User user = new User();

//        TODO remove dummy user in future
        user.setId(1L);
        return user;
    }

    private BigDecimal calculateBookingAmount(Booking booking) {

        BigDecimal pricePerNight =
                booking.getRoom().getBasePrice();

        long days = ChronoUnit.DAYS.between(
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        ) + 1;

        if (days <= 0) {
            throw new IllegalArgumentException(
                    "Check-out date must be after check-in date");
        }

        BigDecimal totalAmount =
                pricePerNight
                        .multiply(BigDecimal.valueOf(days))
                        .multiply(BigDecimal.valueOf(
                                booking.getRoomsCount()
                        ));



        return totalAmount;
    }


}
