package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.entity.ChatRoom;
import com.chatapp.chatservice.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // Angular app port
public class RoomController {

    private final ChatRoomRepository chatRoomRepository;

    @GetMapping("/dm")
    public ResponseEntity<ChatRoom> getOrCreateDMRoom(@RequestParam Long user1, @RequestParam Long user2) {
        // Hamesha choti ID pehle rakhein taaki RoomName humesha same bane (e.g., DM_1_2)
        Long smallerId = Math.min(user1, user2);
        Long largerId = Math.max(user1, user2);
        String uniqueRoomName = "DM_" + smallerId + "_" + largerId;

        // DB mein check karein, agar mil gaya toh return, warna naya banayein
        ChatRoom room = chatRoomRepository.findByRoomName(uniqueRoomName)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.builder()
                            .roomName(uniqueRoomName)
                            .isGroup(false)
                            .build();
                    return chatRoomRepository.save(newRoom);
                });

        return ResponseEntity.ok(room);
    }
}