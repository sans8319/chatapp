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
import org.springframework.messaging.simp.SimpMessagingTemplate;

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

    private final SimpMessagingTemplate messagingTemplate;
    private final com.chatapp.chatservice.service.GroupMessageService groupMessageService;

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

            try {
                messagingTemplate.convertAndSend("/topic/public/updates", Map.of("type", "PROFILE_UPDATED"));
            } catch (Exception e) {
                System.err.println("Notification failed: " + e.getMessage());
            }
            
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

    // =====================================
    // NAYA: DELETE ACCOUNT (SOFT DELETE LOGIC)
    // =====================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            
            // 1. Username ke aage hidden marker lagayein
            if (!user.getUsername().contains("_DELETED_")) {
                user.setUsername(user.getUsername() + "_DELETED_" + id);
            }

            // 2. Sensitive data clear karein
            user.setEmail("deleted_" + id + "@workchat.com");
            user.setPassword("EXPIRED_" + System.currentTimeMillis());
            user.setPhone(null);
            user.setProfilePicture(null);
            
            // 3. Status permanently Offline/Grey karein
            user.setOnline(false);
            user.setStatusState("Offline");
            user.setCustomStatusColor("#94a3b8"); // Grey offline color
            user.setCustomStatusText("Account Deleted");
            user.setAbout("This account is no longer active.");
            user.setDesignation("Former Member");

            userRepository.save(user);
            groupMessageService.removeDeletedUserFromAllGroups(id);

            try {
                messagingTemplate.convertAndSend("/topic/public/updates", 
                    Map.of("type", "USER_DELETED", "userId", id));
            } catch (Exception e) {
                System.err.println("Notification failed: " + e.getMessage());
            }

            return ResponseEntity.ok("Account deleted successfully.");
            
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
    }

    // --- ADD THIS AT THE END OF UserController.java ---
    @PostMapping("/{userId}/toggle-pin/{roomId}")
    public ResponseEntity<?> togglePin(@PathVariable Long userId, @PathVariable String roomId) {
        com.chatapp.chatservice.entity.User user = userRepository.findById(userId).orElseThrow();
        String pinned = user.getPinnedRooms() == null ? "" : user.getPinnedRooms();
        
        // Ex: ",12," ya ",GROUP_4,"
        String token = "," + roomId + ","; 
        
        if (pinned.contains(token)) {
            pinned = pinned.replace(token, ","); // Pehle se hai toh Unpin kardo
        } else {
            if (pinned.isEmpty()) pinned = ",";
            pinned += roomId + ","; // Nahi hai toh Pin kardo
        }
        
        pinned = pinned.replaceAll(",+", ","); // Extra commas clean karein
        if (pinned.equals(",")) pinned = "";
        
        user.setPinnedRooms(pinned);
        userRepository.save(user);
        
        return ResponseEntity.ok(java.util.Map.of("pinnedRooms", pinned));
    }

    // UserController.java mein ye endpoint add karein
    @GetMapping("/departments")
    public ResponseEntity<List<String>> getAllDepartments() {
        List<String> departments = userRepository.findDistinctDepartments();
        return ResponseEntity.ok(departments);
    }
}