package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.dto.UserProfileDTO; // NAYA: DTO import kiya
import com.chatapp.chatservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // Angular app port
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<?> updateUserProfile(@PathVariable Long id, @RequestBody UserProfileDTO profileData) {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            user.setUsername(profileData.getName());
            user.setEmail(profileData.getEmail());
            user.setAbout(profileData.getAbout());
            user.setDepartment(profileData.getDepartment());
            user.setDesignation(profileData.getDesignation());
            user.setLocation(profileData.getLocation());
            user.setPhone(profileData.getPhone());
            user.setProfilePicture(profileData.getProfilePicture());
            user.setStatusState(profileData.getStatusState());
            user.setCustomStatusText(profileData.getCustomStatusText());
            user.setCustomStatusColor(profileData.getCustomStatusColor());
            user.setCountryCode(profileData.getCountryCode());
            userRepository.save(user);
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    // =====================================
    // NAYA: GET SINGLE USER API
    // =====================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (userOptional.isPresent()) {
            return ResponseEntity.ok(userOptional.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }
}