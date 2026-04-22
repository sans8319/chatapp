package com.chatapp.chatservice.controller;
import com.chatapp.chatservice.repository.MessageRepository;
import com.chatapp.chatservice.dto.MessageDTO;
import com.chatapp.chatservice.dto.MessageReceipt;
import com.chatapp.chatservice.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.repository.UserRepository;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    // Jab client '/app/chat.sendMessage' par bhejega
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Message message) {
        message.setTimestamp(java.time.LocalDateTime.now());
        
        // NAYA LOGIC: Sender ki ID se Pura User nikal lo (Crash se bachane ke liye)
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
                .senderId(savedMessage.getSender().getId()) // <--- NAYA LOGIC: Sender ki ID add ki
                .roomId(savedMessage.getChatRoom().getId())
                .timestamp(savedMessage.getTimestamp())
                .delivered(savedMessage.isDelivered())
                .seen(savedMessage.isSeen())
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomId, dto);
    }

    // Apne required repositories inject karna mat bhooliyega (MessageRepository)
    // NAYA CODE: Receipt handler
    @MessageMapping("/chat.receipt")
    public void handleReceipt(MessageReceipt receipt) {
        // 1. Database mein message update karein
        Message msg = messageRepository.findById(receipt.getMessageId()).orElse(null);
        if (msg != null) {
            if ("DELIVERED".equals(receipt.getStatus())) {
                msg.setDelivered(true);
            } else if ("SEEN".equals(receipt.getStatus())) {
                msg.setDelivered(true); // Agar seen hai toh deliver toh hua hi hoga
                msg.setSeen(true);
            }
            messageRepository.save(msg);
            
            // 2. Sender ko wapas WebSocket par update bhej dein
            messagingTemplate.convertAndSend("/topic/room/" + receipt.getRoomId() + "/receipts", receipt);
        }
    }
}