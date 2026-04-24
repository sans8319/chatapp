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
@CrossOrigin(origins = "http://localhost:4200") 
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/create")
    public ResponseEntity<ChatGroup> createGroup(
            @RequestBody GroupCreateRequest request,
            @RequestParam Long creatorId) { 
        
        // NAYA: Ab hum poora 'request' bhej rahe hain taaki saara data service tak jaye
        ChatGroup group = groupService.createGroup(request, creatorId);
        return ResponseEntity.ok(group);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserGroups(@PathVariable Long userId) {
        List<Map<String, Object>> groups = groupService.getUserGroups(userId);
        return ResponseEntity.ok(groups);
    }
}