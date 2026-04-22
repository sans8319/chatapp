package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.dto.MessageDTO;
import com.chatapp.chatservice.repository.MessageRepository;
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

    @GetMapping("/{roomId}")
    public ResponseEntity<List<MessageDTO>> getChatHistory(@PathVariable Long roomId) {
        // DB se us room ke messages nikalo aur DTO mein convert karo
        List<MessageDTO> history = messageRepository.findByChatRoomIdOrderByTimestampAsc(roomId)
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