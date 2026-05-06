package com.logic.Service;

import com.logic.DTO.ProfileUpdateRequestDTO;
import com.logic.DTO.UserDTO;
import com.logic.entity.User;

public interface UserService {

    User getUserById(Long id);

    UserDTO getMyProfile();

    void updateProfile(ProfileUpdateRequestDTO profileUpdateRequestDTO);
}
