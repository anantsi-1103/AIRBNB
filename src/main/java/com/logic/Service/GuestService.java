package com.logic.Service;

import com.logic.DTO.GuestDTO;

import java.util.List;

public interface GuestService {

    List<GuestDTO> getAllGuests();

    GuestDTO addNewGuest(GuestDTO guestDTO);

    void updateGuest(Long guestId, GuestDTO guestDTO);

    void deleteGuest(Long guestId);
}
