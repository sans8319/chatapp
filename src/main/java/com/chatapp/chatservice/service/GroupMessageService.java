package com.chatapp.chatservice.service;

import com.chatapp.chatservice.entity.ChatGroup;
import com.chatapp.chatservice.entity.GroupMessage;
import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.repository.ChatGroupRepository;
import com.chatapp.chatservice.repository.GroupMessageRepository;
import com.chatapp.chatservice.repository.UserRepository; // NAYA: Real name fetch karne ke liye
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GroupMessageService {

    private final GroupMessageRepository groupMessageRepository;
    private final ChatGroupRepository chatGroupRepository;
    private final UserRepository userRepository; // NAYA INJECTION
    private final SimpMessagingTemplate messagingTemplate;

    public GroupMessageService(GroupMessageRepository groupMessageRepository, ChatGroupRepository chatGroupRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.groupMessageRepository = groupMessageRepository;
        this.chatGroupRepository = chatGroupRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // Naya message save karna aur broadcast karna
    public void saveAndBroadcastMessage(Long groupId, Map<String, Object> payload) {
        try {
            ChatGroup group = chatGroupRepository.findById(groupId).orElse(null);
            if (group == null) {
                System.out.println("❌ ERROR: Group not found with ID: " + groupId);
                return;
            }

            GroupMessage msg = new GroupMessage();
            msg.setChatGroup(group);
            msg.setContent((String) payload.get("content"));
            
            // --- NAYA FIX: ROBUST PAYLOAD PARSING ---
            // Ye check karega ki senderId direct aayi hai, ya object ke andar hai (1-on-1 style)
            Long senderId = null;
            if (payload.containsKey("senderId") && payload.get("senderId") != null) {
                senderId = ((Number) payload.get("senderId")).longValue();
            } else if (payload.containsKey("sender")) {
                Map<String, Object> senderMap = (Map<String, Object>) payload.get("sender");
                if (senderMap != null && senderMap.get("id") != null) {
                    senderId = ((Number) senderMap.get("id")).longValue();
                }
            }

            if (senderId == null) {
                System.out.println("❌ ERROR: Sender ID is missing in payload: " + payload);
                return;
            }

            msg.setSenderId(senderId);

            // Database se real sender name nikalna (Bubble ke upar dikhane ke liye)
            User senderUser = userRepository.findById(senderId).orElse(null);
            if (senderUser != null) {
                msg.setSenderName(senderUser.getUsername());
            } else {
                msg.setSenderName((String) payload.getOrDefault("senderName", "Member"));
            }

            // Message DB me save kiya
            GroupMessage savedMsg = groupMessageRepository.save(msg);

            // Message ko frontend format me tayar karo
            Map<String, Object> responseMsg = new HashMap<>();
            responseMsg.put("id", savedMsg.getId());
            responseMsg.put("senderId", savedMsg.getSenderId());
            responseMsg.put("senderName", savedMsg.getSenderName());
            responseMsg.put("content", savedMsg.getContent());
            responseMsg.put("roomId", "GROUP_" + groupId); // Wapas GROUP_ format me bhejo
            responseMsg.put("timestamp", savedMsg.getTimestamp());
            responseMsg.put("seen", true); 

            System.out.println("✅ SUCCESS: Broadcasting group message to: /topic/room/GROUP_" + groupId);
            
            // WebSocket par bhej do
            messagingTemplate.convertAndSend("/topic/room/GROUP_" + groupId, responseMsg);

        } catch (Exception e) {
            System.err.println("❌ CATCH ERROR: Failed to process group message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Frontend ke liye Chat History nikalna
    public List<Map<String, Object>> getGroupHistory(Long groupId) {
        List<GroupMessage> messages = groupMessageRepository.findByChatGroupIdOrderByTimestampAsc(groupId);
        
        return messages.stream().map(msg -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", msg.getId());
            map.put("senderId", msg.getSenderId());
            map.put("senderName", msg.getSenderName());
            map.put("content", msg.getContent());
            map.put("roomId", "GROUP_" + groupId);
            map.put("timestamp", msg.getTimestamp());
            map.put("seen", true);
            return map;
        }).collect(Collectors.toList());
    }
}