package com.logic.security;


import com.logic.DTO.LoginDTO;
import com.logic.DTO.LoginResponseDTO;
import com.logic.DTO.SignUpRequestDTO;
import com.logic.DTO.UserDTO;
import com.logic.Repository.UserRepository;
import com.logic.Service.UserService;
import com.logic.entity.User;
import com.logic.entity.enums.Role;
import com.logic.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;

    public UserDTO signUp(SignUpRequestDTO signUpRequestDTO) {
        User user = userRepository.findByEmail(signUpRequestDTO.getEmail()).orElse(null);

        if (user != null) {
            throw new RuntimeException("User is already present with the same email id: ");
        }

        User newUser = modelMapper.map(signUpRequestDTO, User.class);
        newUser.setRoles(Set.of(Role.GUEST));
        newUser.setPassword(passwordEncoder.encode(signUpRequestDTO.getPassword()));
        newUser = userRepository.save(newUser);

        return modelMapper.map(newUser, UserDTO.class);
    }

    public String[] login(LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken
                        (loginDTO.getEmail(), loginDTO.getPassword()));

        User user = (User) authentication.getPrincipal();

        String[] arr = new String[2];

        arr[0] = jwtService.generateAccessToken(user);
        arr[1] = jwtService.generateRefreshToken(user);

        return arr;
    }

    public String refreshToken(String refreshToken) {
        Long id = jwtService.getUserIdFromToken(refreshToken);


        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return jwtService.generateAccessToken(user);

    }

}








