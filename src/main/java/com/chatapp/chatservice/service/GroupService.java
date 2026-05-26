package com.chatapp.chatservice.service;

import com.chatapp.chatservice.dto.GroupCreateRequest;
import com.chatapp.chatservice.entity.ChatGroup;
import com.chatapp.chatservice.entity.GroupMember;
import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.repository.ChatGroupRepository;
import com.chatapp.chatservice.repository.GroupMessageRepository;
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
    
    private final SimpMessagingTemplate messagingTemplate;
    private final GroupMessageService groupMessageService;

    private final GroupMessageRepository groupMessageRepository;

    public GroupService(ChatGroupRepository groupRepository, GroupMemberRepository groupMemberRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate, GroupMessageService groupMessageService, GroupMessageRepository groupMessageRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.groupMessageService = groupMessageService;
        this.groupMessageRepository = groupMessageRepository;
    }

    @Transactional
    public ChatGroup createGroup(GroupCreateRequest request, Long creatorId) { 
        ChatGroup group = new ChatGroup();
        group.setName(request.getName());
        
        group.setDescription(request.getDescription());
        group.setPermissions(request.getPermissions());
        group.setProfilePicture(request.getProfilePicture());

        group.setCreatedBy(creatorId);
        ChatGroup savedGroup = groupRepository.save(group);

        List<Long> memberIds = request.getMemberIds();
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

        // 1. SYSTEM MESSAGE (Purana Logic Safe Hai)
        Map<String, Object> sysPayload = new HashMap<>();
        sysPayload.put("content", "###GROUP_CREATED###"); 
        sysPayload.put("senderId", creatorId);
        sysPayload.put("senderName", "System");
        sysPayload.put("roomId", "GROUP_" + savedGroup.getId()); 
        
        groupMessageService.saveAndBroadcastMessage(savedGroup.getId(), sysPayload);

        // 2. PERSONAL NOTIFICATIONS (Purana Logic Safe Hai)
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

    public List<Map<String, Object>> getUserGroups(Long userId) {
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
        
        return memberships.stream().map(membership -> {
            Map<String, Object> groupData = new HashMap<>();
            groupData.put("id", membership.getChatGroup().getId());
            groupData.put("username", membership.getChatGroup().getName()); 
            
            groupData.put("description", membership.getChatGroup().getDescription());
            groupData.put("permissions", membership.getChatGroup().getPermissions());
            groupData.put("profilePicture", membership.getChatGroup().getProfilePicture());
            
            groupData.put("isGroup", true); 
            groupData.put("lastMessage", "Tap to start chatting...");
            groupData.put("unreadCount", 0);
            return groupData;
        }).collect(Collectors.toList());
    }

    // 🛑 UPDATED: Naye Promoted Admins ko bhi list me mark karega
    public List<Map<String, Object>> getGroupMembers(Long groupId) {
        ChatGroup group = groupRepository.findById(groupId).orElse(null);
        Long adminId = (group != null) ? group.getCreatedBy() : -1L;
        String promotedAdmins = (group != null && group.getAdminIds() != null) ? group.getAdminIds() : "";

        List<GroupMember> members = groupMemberRepository.findByChatGroupId(groupId);
        return members.stream().map(m -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", m.getUser().getId());
            userMap.put("username", m.getUser().getUsername());
            userMap.put("profilePicture", m.getUser().getProfilePicture());
            userMap.put("online", m.getUser().isOnline());
            userMap.put("customStatusText", m.getUser().getCustomStatusText());
            
            userMap.put("statusState", m.getUser().getStatusState()); 
            userMap.put("customStatusColor", m.getUser().getCustomStatusColor()); 
            
            // NAYA MAGIC: Check karo ki Original Creator hai YA Promoted Admin hai
            boolean isCreator = m.getUser().getId().equals(adminId);
            boolean isPromoted = promotedAdmins.contains("," + m.getUser().getId() + ",");
            userMap.put("isAdmin", isCreator || isPromoted);
            userMap.put("isCreator", isCreator);
            
            return userMap;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void addMembersToGroup(Long groupId, List<Long> userIds, Long addedById) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        User addedBy = userRepository.findById(addedById).orElse(null);
        String adderName = (addedBy != null) ? addedBy.getUsername() : "Someone";

        List<GroupMember> existingMembers = groupMemberRepository.findByChatGroupId(groupId);
        List<Long> existingUserIds = existingMembers.stream().map(m -> m.getUser().getId()).collect(Collectors.toList());

        for (Long userId : userIds) {
            if (!existingUserIds.contains(userId)) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    GroupMember member = new GroupMember();
                    member.setChatGroup(group);
                    member.setUser(user);
                    groupMemberRepository.save(member);

                    // Map<String, Object> sysPayload = new HashMap<>();
                    // sysPayload.put("content", adderName + " added " + user.getUsername() + ".");
                    // sysPayload.put("senderId", addedById);
                    // sysPayload.put("senderName", "System");
                    // sysPayload.put("roomId", "GROUP_" + groupId);
                    // groupMessageService.saveAndBroadcastMessage(groupId, sysPayload);

                    Map<String, String> notification = new HashMap<>();
                    notification.put("type", "NEW_GROUP");
                    try {
                        messagingTemplate.convertAndSend("/topic/user/" + userId, notification);
                    } catch (Exception e) { }
                }
            }
        }
    }

    // ==========================================
    // 🛑 NAYA: MAKE ADMIN LOGIC
    // ==========================================
    @Transactional
    public void makeAdmin(Long groupId, Long userId) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        String admins = group.getAdminIds() == null ? "" : group.getAdminIds();
        
        String token = "," + userId + ",";
        if (!admins.contains(token)) {
            if (admins.isEmpty()) admins = ",";
            admins += userId + ",";
            group.setAdminIds(admins);
            groupRepository.save(group); // Database me hamesha ke liye save
        }
        
        // Realtime WebSocket Notification dusre users ke panel update karne ke liye
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "ADMIN_PROMOTED");
        wsMsg.put("userId", userId);
        wsMsg.put("roomId", "GROUP_" + groupId);
        
        try {
            messagingTemplate.convertAndSend("/topic/room/GROUP_" + groupId, wsMsg);
        } catch (Exception e) {
            System.err.println("Admin promotion broadcast failed: " + e.getMessage());
        }
    }

    // 🛑 NAYA: Dismiss Admin Logic
    @Transactional
    public void dismissAdmin(Long groupId, Long userId) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        String admins = group.getAdminIds() == null ? "" : group.getAdminIds();
        
        String token = "," + userId + ",";
        if (admins.contains(token)) {
            admins = admins.replace(token, ","); // Remove kar do
            if (admins.equals(",")) admins = "";
            group.setAdminIds(admins);
            groupRepository.save(group); // Database me update
        }
        
        // Realtime signal bhejo
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "ADMIN_DISMISSED");
        wsMsg.put("userId", userId);
        wsMsg.put("roomId", "GROUP_" + groupId);
        try {
            messagingTemplate.convertAndSend("/topic/room/GROUP_" + groupId, wsMsg);
        } catch (Exception e) {}
    }

    // ==========================================
    // 🛑 NAYA: UPDATE GROUP PERMISSIONS REAL-TIME
    // ==========================================
    @Transactional
    public void updateGroupPermissions(Long groupId, String permissions) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        group.setPermissions(permissions);
        groupRepository.save(group);

        // Real-time WebSocket signal bhejo saare users ko
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "GROUP_SETTINGS_UPDATED");
        wsMsg.put("groupId", groupId);
        wsMsg.put("permissions", permissions);
        wsMsg.put("roomId", "GROUP_" + groupId);
        
        try {
            messagingTemplate.convertAndSend("/topic/room/GROUP_" + groupId, wsMsg);
        } catch (Exception e) {
            System.err.println("Permission broadcast failed: " + e.getMessage());
        }
    }

   // ==========================================
    // 🛑 NAYA: DELETE GROUP & BROADCAST
    // ==========================================
    @Transactional
    public void deleteGroup(Long groupId) {
        // 1. Pehle sabhi ko signal bhej do taaki group real-time me screen se hat jaye
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "GROUP_DELETED");
        wsMsg.put("groupId", groupId);
        wsMsg.put("roomId", "GROUP_" + groupId);
        
        try {
            messagingTemplate.convertAndSend("/topic/room/GROUP_" + groupId, wsMsg);
            messagingTemplate.convertAndSend("/topic/public/updates", wsMsg);
        } catch (Exception e) {}

        // 2. Database se sab kuch line se saaf karo (Pehle messages, fir members, aakhir me group)
        groupMessageService.deleteAllMessagesOfGroup(groupId); // Messages saaf
        groupMemberRepository.deleteByGroupId(groupId);        // Members saaf
        groupRepository.deleteById(groupId);                   // Group saaf
    }
}