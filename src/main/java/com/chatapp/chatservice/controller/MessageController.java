package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.dto.MessageDTO;
import com.chatapp.chatservice.repository.MessageRepository;
import com.chatapp.chatservice.service.GroupMessageService; // NAYA IMPORT
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MessageController {

    private final MessageRepository messageRepository;
    private final GroupMessageService groupMessageService; // NAYA INJECTION

    // NAYA FIX: Long ko String kiya taaki 'GROUP_1' bhi read ho sake
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getChatHistory(@PathVariable String roomId) { 
        
        // --- NAYA GROUP INTERCEPTOR ---
        if (roomId != null && roomId.startsWith("GROUP_")) {
            Long groupId = Long.parseLong(roomId.substring(6));
            return ResponseEntity.ok(groupMessageService.getGroupHistory(groupId));
        }

        // --- AAPKA PURANA 1-ON-1 LOGIC (As it is) ---
        Long parsedRoomId = Long.parseLong(roomId); // Wapas Long me convert
        
        List<MessageDTO> history = messageRepository.findByChatRoomIdOrderByTimestampAsc(parsedRoomId)
                .stream()
                .map(msg -> MessageDTO.builder()
                        .id(msg.getId())
                        .content(msg.getContent())
                        .senderUsername(msg.getSender() != null ? msg.getSender().getUsername() : "Unknown")
                        .senderId(msg.getSender() != null ? msg.getSender().getId() : null)
                        .roomId(msg.getChatRoom().getId())
                        .timestamp(msg.getTimestamp())
                        .delivered(msg.isDelivered())
                        .seen(msg.isSeen())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }
}