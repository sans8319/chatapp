package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.dto.GroupCreateRequest;
import com.chatapp.chatservice.entity.ChatGroup;
import com.chatapp.chatservice.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http://localhost:4200") // CORS apne hisaab se set karein
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // Naya group banane ka endpoint
    @PostMapping("/create")
    public ResponseEntity<ChatGroup> createGroup(
            @RequestBody GroupCreateRequest request,
            @RequestParam Long creatorId) { // Ideal case me yeh JWT token se nikalte hain
        
        ChatGroup group = groupService.createGroup(request.getName(), request.getMemberIds(), creatorId);
        return ResponseEntity.ok(group);
    }

    // User ke saare groups fetch karne ka endpoint
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserGroups(@PathVariable Long userId) {
        List<Map<String, Object>> groups = groupService.getUserGroups(userId);
        return ResponseEntity.ok(groups);
    }
}