package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.dto.UserProfileDTO; // NAYA: DTO import kiya
import com.chatapp.chatservice.dto.PasswordChangeDTO; 
import com.chatapp.chatservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Map;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // Angular app port
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
    // 1. Current Password Verify karne ke liye
    @PostMapping("/{id}/verify-password")
    public ResponseEntity<Boolean> verifyPassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String currentPwd = payload.get("password");
        if (currentPwd == null) return ResponseEntity.badRequest().body(false);

        return userRepository.findById(id).map(user -> {
            // NAYA: Smart check jo purane aur naye dono passwords ko support karega
            boolean isMatch = user.getPassword().startsWith("$2a$") 
                ? passwordEncoder.matches(currentPwd, user.getPassword()) 
                : user.getPassword().equals(currentPwd);
                
            return ResponseEntity.ok(isMatch);
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(false));
    }

    // 2. Password Update karne ke liye
    @PutMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody PasswordChangeDTO data) {
        return userRepository.findById(id).map(user -> {
            
            // NAYA: passwordEncoder.matches() use karein
            if (!passwordEncoder.matches(data.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid current password");
            }
            
            // NAYA: Naye password ko seedha save karne ki bajaye usko encode (encrypt) karein
            user.setPassword(passwordEncoder.encode(data.getNewPassword()));
            
            userRepository.save(user);
            return ResponseEntity.ok("Password updated successfully");
            
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
    }
}