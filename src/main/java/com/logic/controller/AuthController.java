package com.logic.controller;

import com.logic.DTO.LoginDTO;
import com.logic.DTO.LoginResponseDTO;
import com.logic.DTO.SignUpRequestDTO;
import com.logic.DTO.UserDTO;
import com.logic.security.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    public final AuthService authService;
    private final HttpServletResponse httpServletResponse;

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signup(@RequestBody SignUpRequestDTO signUpRequestDTO){
        return new ResponseEntity<>(authService.signUp(signUpRequestDTO), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response){
        String[] token = authService.login(loginDTO);

        Cookie cookie = new Cookie("refreshToken", token[1]);
        cookie.setHttpOnly(true);

        httpServletResponse.addCookie(cookie);
        return ResponseEntity.ok(new LoginResponseDTO(token[0]));
    }
}
