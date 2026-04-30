package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.dto.MessageDTO;
import com.chatapp.chatservice.repository.MessageRepository;
import com.chatapp.chatservice.service.GroupMessageService; // NAYA IMPORT
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MessageController {

    private final MessageRepository messageRepository;
    private final GroupMessageService groupMessageService; // NAYA INJECTION

    // NAYA FIX: Long ko String kiya taaki 'GROUP_1' bhi read ho sake
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getChatHistory(@PathVariable String roomId, @RequestParam(required = false) Long userId) { 
        
        if (roomId != null && roomId.startsWith("GROUP_")) {
            Long groupId = Long.parseLong(roomId.substring(6));
            // Group ke history mein bhi frontend se filter laga lena chahiye ya Service update karni hogi.
            return ResponseEntity.ok(groupMessageService.getGroupHistory(groupId));
        }

        Long parsedRoomId = Long.parseLong(roomId); 
        
        List<MessageDTO> history = messageRepository.findByChatRoomIdOrderByTimestampAsc(parsedRoomId)
                .stream()
                // NAYA MAGIC FILTER: Agar clearedBy mein is userId ka zikr hai, toh message list mein nahi aayega!
                .filter(msg -> userId == null || msg.getClearedBy() == null || !msg.getClearedBy().contains("," + userId + ","))
                .map(msg -> MessageDTO.builder()
                        .id(msg.getId())
                        .content(msg.getContent())
                        .senderUsername(msg.getSender() != null ? msg.getSender().getUsername() : "Unknown")
                        .senderId(msg.getSender() != null ? msg.getSender().getId() : null)
                        .roomId(msg.getChatRoom().getId())
                        .timestamp(msg.getTimestamp())
                        .delivered(msg.isDelivered())
                        .seen(msg.isSeen())
                        .fileUrl(msg.getFileUrl())
                        .fileName(msg.getFileName())
                        .fileType(msg.getFileType())
                        .fileSize(msg.getFileSize())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }
    // ==========================================
    // NAYA: SIRF MEDIA AUR LINKS FETCH KARNE KE LIYE
    // ==========================================
    @GetMapping("/{roomId}/media")
    public ResponseEntity<?> getRoomMedia(@PathVariable String roomId, @RequestParam(required = false) Long userId) { // NAYA: Yahan userId parameter add kiya
        if (roomId != null && roomId.startsWith("GROUP_")) {
            Long groupId = Long.parseLong(roomId.substring(6));
            return ResponseEntity.ok(groupMessageService.getGroupMediaAndLinks(groupId));
        }

        Long parsedRoomId = Long.parseLong(roomId);
        List<MessageDTO> mediaHistory = messageRepository.findMediaAndLinksByRoomId(parsedRoomId)
                .stream()
                // NAYA MAGIC FILTER: Jo message is user ne delete kar diya hai, wo media tab me bhi nahi dikhega!
                .filter(msg -> userId == null || msg.getClearedBy() == null || !msg.getClearedBy().contains("," + userId + ","))
                .map(msg -> MessageDTO.builder()
                        .id(msg.getId())
                        .content(msg.getContent())
                        .senderUsername(msg.getSender() != null ? msg.getSender().getUsername() : "Unknown")
                        .roomId(msg.getChatRoom().getId())
                        .timestamp(msg.getTimestamp())
                        .fileUrl(msg.getFileUrl())
                        .fileName(msg.getFileName())
                        .fileType(msg.getFileType())
                        .fileSize(msg.getFileSize())
                        .build())
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(mediaHistory);
    }

    @Transactional
    @DeleteMapping("/{roomId}/clear")
    public ResponseEntity<?> clearChatHistory(@PathVariable String roomId, @RequestParam Long userId) {
        if (roomId.startsWith("GROUP_")) {
            // Hum groups ko abhi as-is chhod sakte hain ya GroupMessageService me update kar sakte hain
        } else {
            Long parsedRoomId = Long.parseLong(roomId);
            // Room ke saare messages nikale
            List<com.chatapp.chatservice.entity.Message> messages = messageRepository.findByChatRoomIdOrderByTimestampAsc(parsedRoomId);
            
            // Har message par is user ki ID tag kar di
            for (com.chatapp.chatservice.entity.Message msg : messages) {
                if (msg.getClearedBy() == null) {
                    msg.setClearedBy("");
                }
                // Agar user ki ID pehle se nahi hai, toh append kar do
                if (!msg.getClearedBy().contains("," + userId + ",")) {
                    msg.setClearedBy(msg.getClearedBy() + "," + userId + ",");
                }
            }
            // Database me save kar diya
            messageRepository.saveAll(messages);
        }
        return ResponseEntity.ok("Chat cleared for user: " + userId);
    }
}