package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    // Jab client '/app/chat.sendMessage' par bhejega
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Message message) {
        String roomId = message.getChatRoom().getId().toString();
        
        // Is room ke sabhi subscribers ko message bhej do
        // Frontend '/topic/room/{roomId}' ko subscribe karega
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }
}