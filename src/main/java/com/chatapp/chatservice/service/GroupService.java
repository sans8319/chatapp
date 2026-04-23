package com.chatapp.chatservice.service;

import com.chatapp.chatservice.entity.ChatGroup;
import com.chatapp.chatservice.entity.GroupMember;
import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.repository.ChatGroupRepository;
import com.chatapp.chatservice.repository.GroupMemberRepository;
import com.chatapp.chatservice.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GroupService {
    private final ChatGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    
    // NAYA: Pings aur System message ke liye
    private final SimpMessagingTemplate messagingTemplate;
    private final GroupMessageService groupMessageService;

    public GroupService(ChatGroupRepository groupRepository, GroupMemberRepository groupMemberRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate, GroupMessageService groupMessageService) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.groupMessageService = groupMessageService;
    }

    @Transactional
    public ChatGroup createGroup(String name, List<Long> memberIds, Long creatorId) {
        ChatGroup group = new ChatGroup();
        group.setName(name);
        group.setCreatedBy(creatorId);
        ChatGroup savedGroup = groupRepository.save(group);

        if (!memberIds.contains(creatorId)) {
            memberIds.add(creatorId);
        }

        for (Long userId : memberIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                GroupMember member = new GroupMember();
                member.setChatGroup(savedGroup);
                member.setUser(user);
                groupMemberRepository.save(member);
            }
        }

        // 1. SYSTEM MESSAGE: Naya group bante hi ek default message daalo ("You were added...")
        Map<String, Object> sysPayload = new HashMap<>();
        sysPayload.put("content", "You were added to this group.");
        sysPayload.put("senderId", creatorId);
        sysPayload.put("senderName", "System");
        // Yeh DB me save hoga aur frontend ko websocket par chala jayega
        groupMessageService.saveAndBroadcastMessage(savedGroup.getId(), sysPayload);

        // 2. PERSONAL NOTIFICATIONS: Saare members ko ping karo ki sidebar update karein
        Map<String, String> notification = new HashMap<>();
        notification.put("type", "NEW_GROUP");
        for (Long userId : memberIds) {
            try {
                messagingTemplate.convertAndSend("/topic/user/" + userId, notification);
            } catch (Exception e) {
                System.err.println("Notification ping failed for user " + userId + ": " + e.getMessage());
            }
        }

        return savedGroup;
    }

    // --- AAPKA ORIGINAL FUNCTION (100% SAFE) ---
    // Frontend ko data bhejne ke liye method
    public List<Map<String, Object>> getUserGroups(Long userId) {
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
        
        return memberships.stream().map(membership -> {
            Map<String, Object> groupData = new HashMap<>();
            groupData.put("id", membership.getChatGroup().getId());
            groupData.put("username", membership.getChatGroup().getName()); // frontend variable se match karne ke liye
            groupData.put("isGroup", true); // Frontend ko batane ke liye ki ye group hai
            groupData.put("lastMessage", "Tap to start chatting...");
            groupData.put("unreadCount", 0);
            return groupData;
        }).collect(Collectors.toList());
    }
}