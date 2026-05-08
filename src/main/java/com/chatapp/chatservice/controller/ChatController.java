package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.repository.MessageRepository;
import com.chatapp.chatservice.dto.MessageDTO;
import com.chatapp.chatservice.dto.MessageReceipt;
import com.chatapp.chatservice.entity.Message;
import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.repository.UserRepository;
import com.chatapp.chatservice.service.GroupMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.HashMap;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    
    private final GroupMessageService groupMessageService; 
    private final ObjectMapper objectMapper; 

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, Object> payload) {
        
        // 1. Room ID nikal kar check karo
        String roomIdStr = null;
        if (payload.containsKey("roomId")) {
            roomIdStr = String.valueOf(payload.get("roomId"));
        } else if (payload.containsKey("chatRoom")) {
            Map<String, Object> chatRoom = (Map<String, Object>) payload.get("chatRoom");
            if (chatRoom != null && chatRoom.containsKey("id")) {
                roomIdStr = String.valueOf(chatRoom.get("id"));
            }
        }

        // --- NAYA GROUP INTERCEPTOR ---
        if (roomIdStr != null && roomIdStr.startsWith("GROUP_")) {
            Long groupId = Long.parseLong(roomIdStr.substring(6));
            groupMessageService.saveAndBroadcastMessage(groupId, payload);
            return; 
        }

        // --- AAPKA PURANA 1-ON-1 LOGIC ---
        Message message = objectMapper.convertValue(payload, Message.class);
        
        message.setTimestamp(java.time.LocalDateTime.now());
        
        if (message.getSender() != null && message.getSender().getId() != null) {
            User sender = userRepository.findById(message.getSender().getId())
                                        .orElseThrow(() -> new RuntimeException("User not found"));
            message.setSender(sender);
        }

        // 🛑 NAYA: Frontend se Reply ka Data Extract karna (1-on-1 ke liye)
        if (payload.containsKey("replyTo") && payload.get("replyTo") != null) {
            Map<String, Object> replyMap = (Map<String, Object>) payload.get("replyTo");
            if (replyMap.get("id") != null) message.setReplyToId(((Number) replyMap.get("id")).longValue());
            if (replyMap.get("senderName") != null) message.setReplyToName((String) replyMap.get("senderName"));
            if (replyMap.get("content") != null) message.setReplyToContent((String) replyMap.get("content"));
            if (replyMap.get("fileUrl") != null) message.setReplyToFileUrl((String) replyMap.get("fileUrl"));
        }

        Message savedMessage = messageRepository.save(message);
        String roomId = savedMessage.getChatRoom().getId().toString();

        // 🛑 NAYA: Real-time Websocket me Reply Data wapas bhejna (1-on-1 ke liye)
        Map<String, Object> repMsg = null;
        if (savedMessage.getReplyToId() != null) {
            repMsg = new HashMap<>();
            repMsg.put("id", savedMessage.getReplyToId());
            repMsg.put("senderName", savedMessage.getReplyToName());
            repMsg.put("content", savedMessage.getReplyToContent());
            repMsg.put("fileUrl", savedMessage.getReplyToFileUrl());
        }

        MessageDTO dto = MessageDTO.builder()
                .id(savedMessage.getId())
                .content(savedMessage.getContent())
                .senderUsername(savedMessage.getSender().getUsername())
                .senderId(savedMessage.getSender().getId()) 
                .roomId(savedMessage.getChatRoom().getId())
                .timestamp(savedMessage.getTimestamp())
                .delivered(savedMessage.isDelivered())
                .seen(savedMessage.isSeen())
                .fileUrl(savedMessage.getFileUrl())
                .fileName(savedMessage.getFileName())
                .fileType(savedMessage.getFileType())
                .fileSize(savedMessage.getFileSize())
                .isDeleted(savedMessage.isDeleted()) 
                .isPinned(savedMessage.isPinned())
                .replyTo(repMsg) 
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomId, dto);
    }

    // AAPKA PURANA RECEIPT HANDLER (As it is)
    @MessageMapping("/chat.receipt")
    public void handleReceipt(MessageReceipt receipt) {
        Message msg = messageRepository.findById(receipt.getMessageId()).orElse(null);
        if (msg != null) {
            if ("DELIVERED".equals(receipt.getStatus())) {
                msg.setDelivered(true);
            } else if ("SEEN".equals(receipt.getStatus())) {
                msg.setDelivered(true); 
                msg.setSeen(true);
            }
            messageRepository.save(msg);
            
            messagingTemplate.convertAndSend("/topic/room/" + receipt.getRoomId() + "/receipts", receipt);
        }
    }
}