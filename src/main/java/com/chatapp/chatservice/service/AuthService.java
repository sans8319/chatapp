package com.chatapp.chatservice.service;

import com.chatapp.chatservice.dto.AuthRequest;
import com.chatapp.chatservice.dto.AuthResponse;
import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.repository.UserRepository;
import com.chatapp.chatservice.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate; // NAYA IMPORT
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map; // NAYA IMPORT

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final SimpMessagingTemplate messagingTemplate; // NAYA INJECTION

    public AuthResponse register(AuthRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .online(false)
                .build();
        userRepository.save(user);

        // --- NAYA: Sabhi online users ko alert bhejo ki naya user aa gaya hai ---
        try {
            messagingTemplate.convertAndSend("/topic/public/updates", Map.of("type", "NEW_USER"));
        } catch (Exception e) {
            System.err.println("Notification failed: " + e.getMessage());
        }
        
        String token = jwtProvider.generateToken(new UsernamePasswordAuthenticationToken(user.getUsername(), null));
        return new AuthResponse(token, user.getUsername(), user.getId());
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
        return new AuthResponse(token, user.getUsername(), user.getId());
    }
}