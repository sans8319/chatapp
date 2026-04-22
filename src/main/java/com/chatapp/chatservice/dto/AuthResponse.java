package com.chatapp.chatservice.dto;

import lombok.*;

@Data
@NoArgsConstructor // JSON parsing ke liye zaroori
@AllArgsConstructor
@Builder // Optional, par DTOs initialize karne mein asani hoti hai
public class AuthResponse {
    private String token;
    private String username;
    private Long id;
}