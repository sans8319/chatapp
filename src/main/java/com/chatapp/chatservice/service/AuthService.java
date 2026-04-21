package com.chatapp.chatservice.service;

import com.chatapp.chatservice.dto.AuthRequest;
import com.chatapp.chatservice.dto.AuthResponse;
import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.repository.UserRepository;
import com.chatapp.chatservice.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthResponse register(AuthRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .online(false)
                .build();
        userRepository.save(user);
        
        String token = jwtProvider.generateToken(new UsernamePasswordAuthenticationToken(user.getUsername(), null));
        return new AuthResponse(token, user.getUsername());
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        user.setOnline(true);
        userRepository.save(user);

        String token = jwtProvider.generateToken(new UsernamePasswordAuthenticationToken(user.getUsername(), null));
        return new AuthResponse(token, user.getUsername());
    }
}