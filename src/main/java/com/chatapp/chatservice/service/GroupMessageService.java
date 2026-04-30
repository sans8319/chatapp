package com.chatapp.chatservice.service;

import com.chatapp.chatservice.entity.ChatGroup;
import com.chatapp.chatservice.entity.GroupMessage;
import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.repository.ChatGroupRepository;
import com.chatapp.chatservice.repository.GroupMemberRepository;
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
    private final GroupMemberRepository groupMemberRepository;

    public GroupMessageService(GroupMessageRepository groupMessageRepository, 
                               ChatGroupRepository chatGroupRepository, 
                               UserRepository userRepository, 
                               SimpMessagingTemplate messagingTemplate,
                               GroupMemberRepository groupMemberRepository) { // NAYA INJECT KIYA
        this.groupMessageRepository = groupMessageRepository;
        this.chatGroupRepository = chatGroupRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.groupMemberRepository = groupMemberRepository;
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

            if (payload.containsKey("fileUrl")) msg.setFileUrl((String) payload.get("fileUrl"));
            if (payload.containsKey("fileName")) msg.setFileName((String) payload.get("fileName"));
            if (payload.containsKey("fileType")) msg.setFileType((String) payload.get("fileType"));
            if (payload.containsKey("fileSize") && payload.get("fileSize") != null) {
                Object sizeObj = payload.get("fileSize");
                if (sizeObj instanceof Number) {
                    msg.setFileSize(((Number) sizeObj).longValue());
                } else {
                    // Agar string form me aaya hai toh usko parse kar lo
                    msg.setFileSize(Long.parseLong(sizeObj.toString()));
                }
            }
            
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
            responseMsg.put("fileUrl", savedMsg.getFileUrl());
            responseMsg.put("fileName", savedMsg.getFileName());
            responseMsg.put("fileType", savedMsg.getFileType());
            responseMsg.put("fileSize", savedMsg.getFileSize());

            System.out.println("✅ SUCCESS: Broadcasting group message to: /topic/room/GROUP_" + groupId);
            
            // WebSocket par bhej do
            messagingTemplate.convertAndSend("/topic/room/GROUP_" + groupId, responseMsg);

        } catch (Exception e) {
            System.err.println("❌ CATCH ERROR: Failed to process group message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Frontend ke liye Chat History nikalna
    public List<Map<String, Object>> getGroupHistory(Long groupId, Long userId) { // NAYA: userId add kiya
        List<GroupMessage> messages = groupMessageRepository.findByChatGroupIdOrderByTimestampAsc(groupId);
        
        return messages.stream()
                // NAYA MAGIC FILTER: Group messages ke liye
                .filter(msg -> userId == null || msg.getClearedBy() == null || !msg.getClearedBy().contains("," + userId + ","))
                .map(msg -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", msg.getId());
                    map.put("senderId", msg.getSenderId());
                    map.put("senderName", msg.getSenderName());
                    map.put("content", msg.getContent());
                    map.put("roomId", "GROUP_" + groupId);
                    map.put("timestamp", msg.getTimestamp());
                    map.put("seen", true);
                    map.put("fileUrl", msg.getFileUrl());
                    map.put("fileName", msg.getFileName());
                    map.put("fileType", msg.getFileType());
                    map.put("fileSize", msg.getFileSize());
                    return map;
                }).collect(Collectors.toList());
    }

    // NAYA: Frontend ke liye sirf Media aur Links nikalna
    public List<java.util.Map<String, Object>> getGroupMediaAndLinks(Long groupId, Long userId) { // NAYA: userId add kiya
        List<GroupMessage> messages = groupMessageRepository.findMediaAndLinksByGroupId(groupId);
        
        return messages.stream()
                // NAYA MAGIC FILTER: Group media ke liye
                .filter(msg -> userId == null || msg.getClearedBy() == null || !msg.getClearedBy().contains("," + userId + ","))
                .map(msg -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", msg.getId());
                    map.put("senderId", msg.getSenderId());
                    map.put("senderName", msg.getSenderName());
                    map.put("content", msg.getContent());
                    map.put("roomId", "GROUP_" + groupId);
                    map.put("timestamp", msg.getTimestamp());
                    map.put("seen", true);
                    map.put("fileUrl", msg.getFileUrl());
                    map.put("fileName", msg.getFileName());
                    map.put("fileType", msg.getFileType());
                    map.put("fileSize", msg.getFileSize());
                    return map;
                }).collect(Collectors.toList());
    }

    // ==========================================
    // UPDATED: GROUP CHAT CLEAR & HARD DELETE LOGIC
    // ==========================================
    @org.springframework.transaction.annotation.Transactional
    public void clearGroupChat(Long groupId, Long userId) {
        long totalMembers = groupMemberRepository.countByChatGroupId(groupId);
        if (totalMembers == 0) return; 

        List<GroupMessage> messages = groupMessageRepository.findByChatGroupIdOrderByTimestampAsc(groupId);
        List<GroupMessage> toDelete = new java.util.ArrayList<>();
        List<GroupMessage> toSave = new java.util.ArrayList<>();

        for (GroupMessage msg : messages) {
            
            // NAYA MAGIC: System messages ko ignore karo taaki badge delete na ho!
            boolean isSystemMsg = "System".equals(msg.getSenderName()) || 
                                  "###GROUP_CREATED###".equals(msg.getContent()) || 
                                  "You were added to this group.".equals(msg.getContent());
                                  
            if (isSystemMsg) {
                toSave.add(msg); // Isko safe list mein daalo
                continue;        // Aur agle message par badh jao (taaki ispe clearedBy ka tag na lage)
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

            if (count >= totalMembers) {
                toDelete.add(msg);
            } else {
                toSave.add(msg);
            }
        }

        groupMessageRepository.deleteAll(toDelete);
        groupMessageRepository.saveAll(toSave);
    }
}