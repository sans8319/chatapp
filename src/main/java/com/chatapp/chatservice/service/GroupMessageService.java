package com.chatapp.chatservice.service;

import com.chatapp.chatservice.entity.ChatGroup;
import com.chatapp.chatservice.entity.GroupMessage;
import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.repository.ChatGroupRepository;
import com.chatapp.chatservice.repository.GroupMemberRepository;
import com.chatapp.chatservice.repository.GroupMessageRepository;
import com.chatapp.chatservice.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GroupMemberRepository groupMemberRepository;

    public GroupMessageService(GroupMessageRepository groupMessageRepository, 
                               ChatGroupRepository chatGroupRepository, 
                               UserRepository userRepository, 
                               SimpMessagingTemplate messagingTemplate,
                               GroupMemberRepository groupMemberRepository) { 
        this.groupMessageRepository = groupMessageRepository;
        this.chatGroupRepository = chatGroupRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.groupMemberRepository = groupMemberRepository;
    }

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
                    msg.setFileSize(Long.parseLong(sizeObj.toString()));
                }
            }
            
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

            User senderUser = userRepository.findById(senderId).orElse(null);
            if (senderUser != null) {
                msg.setSenderName(senderUser.getUsername());
            } else {
                msg.setSenderName((String) payload.getOrDefault("senderName", "Member"));
            }

            // 🛑 NAYA: Frontend se Reply ka Data Extract karna
            if (payload.containsKey("replyTo") && payload.get("replyTo") != null) {
                Map<String, Object> replyMap = (Map<String, Object>) payload.get("replyTo");
                if (replyMap.get("id") != null) msg.setReplyToId(((Number) replyMap.get("id")).longValue());
                if (replyMap.get("senderName") != null) msg.setReplyToName((String) replyMap.get("senderName"));
                if (replyMap.get("content") != null) msg.setReplyToContent((String) replyMap.get("content"));
                if (replyMap.get("fileUrl") != null) msg.setReplyToFileUrl((String) replyMap.get("fileUrl"));
            }

            GroupMessage savedMsg = groupMessageRepository.save(msg);

            Map<String, Object> responseMsg = new HashMap<>();
            responseMsg.put("id", savedMsg.getId());
            responseMsg.put("senderId", savedMsg.getSenderId());
            responseMsg.put("senderName", savedMsg.getSenderName());
            responseMsg.put("content", savedMsg.getContent());
            responseMsg.put("roomId", "GROUP_" + groupId); 
            responseMsg.put("timestamp", savedMsg.getTimestamp());
            responseMsg.put("seen", true); 
            responseMsg.put("fileUrl", savedMsg.getFileUrl());
            responseMsg.put("fileName", savedMsg.getFileName());
            responseMsg.put("fileType", savedMsg.getFileType());
            responseMsg.put("fileSize", savedMsg.getFileSize());
            responseMsg.put("isDeleted", false);

            // 🛑 NAYA: Real-time Websocket me Reply Data wapas bhejna
            if (savedMsg.getReplyToId() != null) {
                Map<String, Object> repMsg = new HashMap<>();
                repMsg.put("id", savedMsg.getReplyToId());
                repMsg.put("senderName", savedMsg.getReplyToName());
                repMsg.put("content", savedMsg.getReplyToContent());
                repMsg.put("fileUrl", savedMsg.getReplyToFileUrl());
                responseMsg.put("replyTo", repMsg);
            }

            messagingTemplate.convertAndSend("/topic/room/GROUP_" + groupId, responseMsg);

        } catch (Exception e) {
            System.err.println("❌ CATCH ERROR: Failed to process group message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> getGroupHistory(Long groupId, Long userId) { 
        List<GroupMessage> messages = groupMessageRepository.findByChatGroupIdOrderByTimestampAsc(groupId);
        
        return messages.stream()
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
                    map.put("isDeleted", msg.isDeleted()); // 🛑 NAYA: Bhejo
                    // 🛑 NAYA: Chat History load hote waqt Reply Map bhejna
                    if (msg.getReplyToId() != null) {
                        Map<String, Object> repMsg = new HashMap<>();
                        repMsg.put("id", msg.getReplyToId());
                        repMsg.put("senderName", msg.getReplyToName());
                        repMsg.put("content", msg.getReplyToContent());
                        repMsg.put("fileUrl", msg.getReplyToFileUrl());
                        map.put("replyTo", repMsg);
                    }
                    return map;
                }).collect(Collectors.toList());
    }

    public List<java.util.Map<String, Object>> getGroupMediaAndLinks(Long groupId, Long userId) { 
        List<GroupMessage> messages = groupMessageRepository.findMediaAndLinksByGroupId(groupId);
        
        return messages.stream()
                // 🛑 NAYA MAGIC FILTER: Deleted messages Media tab me nahi aayenge
                .filter(msg -> !msg.isDeleted() && (userId == null || msg.getClearedBy() == null || !msg.getClearedBy().contains("," + userId + ",")))
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

    @org.springframework.transaction.annotation.Transactional
    public void clearGroupChat(Long groupId, Long userId) {
        long totalMembers = groupMemberRepository.countByChatGroupId(groupId);
        if (totalMembers == 0) return; 

        List<GroupMessage> messages = groupMessageRepository.findByChatGroupIdOrderByTimestampAsc(groupId);
        List<GroupMessage> toDelete = new java.util.ArrayList<>();
        List<GroupMessage> toSave = new java.util.ArrayList<>();

        for (GroupMessage msg : messages) {
            
            boolean isSystemMsg = "System".equals(msg.getSenderName()) || 
                                  "###GROUP_CREATED###".equals(msg.getContent()) || 
                                  "You were added to this group.".equals(msg.getContent());
                                  
            if (isSystemMsg) {
                toSave.add(msg); 
                continue;        
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

    // ==========================================
    // 🛑 NAYA: SOFT DELETE FOR GROUP MESSAGES
    // ==========================================
    @org.springframework.transaction.annotation.Transactional
    public void softDeleteGroupMessage(Long messageId, Long groupId) {
        GroupMessage msg = groupMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Group message not found"));

        msg.setDeleted(true);
        msg.setContent("This message is deleted");
        msg.setFileUrl(null); 
        msg.setFileName(null);
        msg.setFileType(null);
        msg.setFileSize(null);
        groupMessageRepository.save(msg);

        // Real-time broadcast karo
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "MESSAGE_DELETED");
        wsMsg.put("messageId", messageId);
        wsMsg.put("roomId", "GROUP_" + groupId);
        
        messagingTemplate.convertAndSend("/topic/room/GROUP_" + groupId, wsMsg);
    }
}