package com.logic.Service;

import com.logic.DTO.GuestDTO;
import com.logic.Repository.GuestRepository;
import com.logic.entity.Guest;
import com.logic.entity.User;
import com.logic.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.logic.utils.AppUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestServiceImpl implements GuestService {

    private final GuestRepository guestRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<GuestDTO> getAllGuests() {
        User user = getCurrentUser();
        log.info("Fetching all guests of user with id: {}", user.getId());
        return guestRepository.findByUser(user)
                .stream()
                .map(guest -> modelMapper.map(guest, GuestDTO.class))
                .toList();
    }

    @Override
    public GuestDTO addNewGuest(GuestDTO guestDTO) {
        User user = getCurrentUser();
        Guest guest = modelMapper.map(guestDTO, Guest.class);
        guest.setId(null);
        guest.setUser(user);

        Guest savedGuest = guestRepository.save(guest);
        log.info("Guest added with id: {}", savedGuest.getId());
        return modelMapper.map(savedGuest, GuestDTO.class);
    }

    @Override
    public void updateGuest(Long guestId, GuestDTO guestDTO) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + guestId));
        User user = getCurrentUser();

        if (!user.getId().equals(guest.getUser().getId())) {
            throw new AccessDeniedException("You are not the owner of this guest");
        }

        modelMapper.map(guestDTO, guest);
        guest.setId(guestId);
        guest.setUser(user);
        guestRepository.save(guest);
    }

    @Override
    public void deleteGuest(Long guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + guestId));
        User user = getCurrentUser();

        if (!user.getId().equals(guest.getUser().getId())) {
            throw new AccessDeniedException("You are not the owner of this guest");
        }

        guestRepository.deleteById(guestId);
    }
}
