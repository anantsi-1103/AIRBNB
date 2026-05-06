package com.logic.controller;

import com.logic.DTO.GuestDTO;
import com.logic.DTO.PaymentDTO;
import com.logic.Repository.GuestRepository;
import com.logic.Service.PaymentService;
import com.logic.entity.Guest;
import com.logic.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final PaymentService paymentService;
    private final GuestRepository guestRepository;

    @GetMapping("/me/payments")
    public ResponseEntity<List<PaymentDTO>> getMyPayments() {
        return ResponseEntity.ok(paymentService.getCurrentUserPayments());
    }

    @GetMapping("/me/guests")
    public ResponseEntity<List<GuestDTO>> getMyGuests() {
        User user = getCurrentUser();
        return ResponseEntity.ok(guestRepository.findByUserId(user.getId())
                .stream()
                .map(this::toGuestDTO)
                .toList());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("Authenticated user not found");
        }

        return user;
    }

    private GuestDTO toGuestDTO(Guest guest) {
        GuestDTO guestDTO = new GuestDTO();
        guestDTO.setId(guest.getId());
        guestDTO.setName(guest.getName());
        guestDTO.setGender(guest.getGender());
        guestDTO.setAge(guest.getAge());
        return guestDTO;
    }
}
