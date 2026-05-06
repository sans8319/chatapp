package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.dto.MessageDTO;
import com.chatapp.chatservice.repository.MessageRepository;
import com.chatapp.chatservice.entity.Message;
import com.chatapp.chatservice.service.GroupMessageService; 
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MessageController {

    private final MessageRepository messageRepository;
    private final GroupMessageService groupMessageService; 
    private final SimpMessagingTemplate messagingTemplate;

    // NAYA FIX: Long ko String kiya taaki 'GROUP_1' bhi read ho sake
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getChatHistory(@PathVariable String roomId, @RequestParam(required = false) Long userId) { 
        
        if (roomId != null && roomId.startsWith("GROUP_")) {
            Long groupId = Long.parseLong(roomId.substring(6));
            // Group ke history mein bhi frontend se filter laga lena chahiye ya Service update karni hogi.
            return ResponseEntity.ok(groupMessageService.getGroupHistory(groupId, userId));
        }

        Long parsedRoomId = Long.parseLong(roomId); 
        
        List<MessageDTO> history = messageRepository.findByChatRoomIdOrderByTimestampAsc(parsedRoomId)
                .stream()
                // NAYA MAGIC FILTER: Agar clearedBy mein is userId ka zikr hai, toh message list mein nahi aayega!
                .filter(msg -> userId == null || msg.getClearedBy() == null || !msg.getClearedBy().contains("," + userId + ","))
                .map(msg -> {
                    // 🛑 NAYA MAGIC: Reply Object ko DTO ke liye taiyaar karna
                    Map<String, Object> replyMap = null;
                    if (msg.getReplyToId() != null) {
                        replyMap = new HashMap<>();
                        replyMap.put("id", msg.getReplyToId());
                        replyMap.put("senderName", msg.getReplyToName());
                        replyMap.put("content", msg.getReplyToContent());
                        replyMap.put("fileUrl", msg.getReplyToFileUrl());
                    }

                    return MessageDTO.builder()
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
                        .isDeleted(msg.isDeleted())
                        .replyTo(replyMap) // 🛑 NAYA: Mapper me add kar diya
                        .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }
    
    // ==========================================
    // NAYA: SIRF MEDIA AUR LINKS FETCH KARNE KE LIYE
    // ==========================================
    @GetMapping("/{roomId}/media")
    public ResponseEntity<?> getRoomMedia(@PathVariable String roomId, @RequestParam(required = false) Long userId) { 
        if (roomId != null && roomId.startsWith("GROUP_")) {
            Long groupId = Long.parseLong(roomId.substring(6));
            return ResponseEntity.ok(groupMessageService.getGroupMediaAndLinks(groupId, userId));
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
    
    // ==========================================
    // UPDATED: CLEAR CHAT ROUTE (Hard & Soft Delete)
    // ==========================================
    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/{roomId}/clear")
    public ResponseEntity<?> clearChatHistory(@PathVariable String roomId, @RequestParam Long userId) {
        
        if (roomId.startsWith("GROUP_")) {
            Long groupId = Long.parseLong(roomId.substring(6));
            groupMessageService.clearGroupChat(groupId, userId); 
        } else {
            Long parsedRoomId = Long.parseLong(roomId);
            List<com.chatapp.chatservice.entity.Message> messages = messageRepository.findByChatRoomIdOrderByTimestampAsc(parsedRoomId);
            
            List<com.chatapp.chatservice.entity.Message> toDelete = new java.util.ArrayList<>();
            List<com.chatapp.chatservice.entity.Message> toSave = new java.util.ArrayList<>();

            for (com.chatapp.chatservice.entity.Message msg : messages) {
                
                // NAYA MAGIC: System messages ko yahan bhi bacha lo!
                boolean isSystemMsg = "###GROUP_CREATED###".equals(msg.getContent()) || 
                                      (msg.getSender() != null && "System".equals(msg.getSender().getUsername()));
                                      
                if (isSystemMsg) {
                    toSave.add(msg);
                    continue; // Skip tagging this message
                }

                String cleared = msg.getClearedBy() == null ? "" : msg.getClearedBy();
                String token = "," + userId + ",";

                if (!cleared.contains(token)) {
                    cleared += token;
                    msg.setClearedBy(cleared);
                }

                int count = 0;
                for (String id : cleared.split(",")) {
                    if (!id.trim().isEmpty()) count++;
                }

                if (count >= 2) {
                    toDelete.add(msg);
                } else {
                    toSave.add(msg);
                }
            }
            
            messageRepository.deleteAll(toDelete);
            messageRepository.saveAll(toSave);
        }
        return ResponseEntity.ok("Chat cleared and memory optimized for user: " + userId);
    }

    // 🛑 UPDATED: Soft Delete Message API (Handles BOTH 1-on-1 and Groups)
    @PutMapping("/{messageId}/soft-delete")
    public ResponseEntity<?> softDeleteMessage(@PathVariable Long messageId, @RequestParam String roomId) {
        
        if (roomId != null && roomId.startsWith("GROUP_")) {
            // Agar group ka message hai, toh service me bhej do
            Long groupId = Long.parseLong(roomId.substring(6));
            groupMessageService.softDeleteGroupMessage(messageId, groupId);
        } else {
            // Agar 1-on-1 chat ka message hai
            Message msg = messageRepository.findById(messageId).orElseThrow(() -> new RuntimeException("Message not found"));
            
            msg.setDeleted(true);
            msg.setContent("This message is deleted");
            msg.setFileUrl(null); 
            msg.setFileName(null);
            msg.setFileType(null);
            msg.setFileSize(null);
            messageRepository.save(msg);

            // Real-time WebSocket signal sabko bhejo
            Map<String, Object> wsMsg = new HashMap<>();
            wsMsg.put("type", "MESSAGE_DELETED");
            wsMsg.put("messageId", messageId);
            wsMsg.put("roomId", roomId);
            
            messagingTemplate.convertAndSend("/topic/room/" + roomId, wsMsg);
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Deleted for everyone"));
    }
}