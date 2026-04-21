package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // Angular app port
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public List<User> getAllUsers() {
        // Database se saare users fetch honge
        return userRepository.findAll();
  }
}