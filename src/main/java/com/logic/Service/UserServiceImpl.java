package com.logic.Service;

import com.logic.DTO.ProfileUpdateRequestDTO;
import com.logic.DTO.UserDTO;
import com.logic.Repository.UserRepository;
import com.logic.entity.User;
import com.logic.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User not found with id :"+id));
    }

    @Override
    public UserDTO getMyProfile() {
        return modelMapper.map(com.logic.utils.AppUtils.getCurrentUser(), UserDTO.class);
    }

    @Override
    public void updateProfile(ProfileUpdateRequestDTO profileUpdateRequestDTO) {
        User user = com.logic.utils.AppUtils.getCurrentUser();
        modelMapper.map(profileUpdateRequestDTO, user);
        userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username).orElse(null);
    }


}
