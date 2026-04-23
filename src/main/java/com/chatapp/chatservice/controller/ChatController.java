package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.repository.MessageRepository;
import com.chatapp.chatservice.dto.MessageDTO;
import com.chatapp.chatservice.dto.MessageReceipt;
import com.chatapp.chatservice.entity.Message;
import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.repository.UserRepository;
import com.chatapp.chatservice.service.GroupMessageService; // NAYA IMPORT
import com.fasterxml.jackson.databind.ObjectMapper; // NAYA IMPORT (Safety ke liye)
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    
    private final GroupMessageService groupMessageService; // NAYA INJECTION
    private final ObjectMapper objectMapper; // NAYA INJECTION

    // NAYA FIX: Payload ko Map me liya taaki error na aaye
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
            return; // Group ka process khatam, yahi se wapas!
        }

        // --- AAPKA PURANA 1-ON-1 LOGIC (As it is, 100% Safe) ---
        // Payload ko wapas aapki 'Message' entity me daal diya taaki aapka code chalta rahe
        Message message = objectMapper.convertValue(payload, Message.class);
        
        message.setTimestamp(java.time.LocalDateTime.now());
        
        if (message.getSender() != null && message.getSender().getId() != null) {
            User sender = userRepository.findById(message.getSender().getId())
                                        .orElseThrow(() -> new RuntimeException("User not found"));
            message.setSender(sender);
        }

        Message savedMessage = messageRepository.save(message);
        String roomId = message.getChatRoom().getId().toString();

        MessageDTO dto = MessageDTO.builder()
                .id(savedMessage.getId())
                .content(savedMessage.getContent())
                .senderUsername(savedMessage.getSender().getUsername())
                .senderId(savedMessage.getSender().getId()) 
                .roomId(savedMessage.getChatRoom().getId())
                .timestamp(savedMessage.getTimestamp())
                .delivered(savedMessage.isDelivered())
                .seen(savedMessage.isSeen())
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