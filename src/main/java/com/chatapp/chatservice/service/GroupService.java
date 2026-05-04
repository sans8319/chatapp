package com.chatapp.chatservice.service;

import com.chatapp.chatservice.dto.GroupCreateRequest; // NAYA IMPORT
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
    public ChatGroup createGroup(GroupCreateRequest request, Long creatorId) { // NAYA: Ab poora request object aayega
        ChatGroup group = new ChatGroup();
        group.setName(request.getName());
        
        // --- NAYE FIELDS DB MEIN SAVE HO RAHE HAIN ---
        group.setDescription(request.getDescription());
        group.setPermissions(request.getPermissions());
        group.setProfilePicture(request.getProfilePicture());
        // ---------------------------------------------

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
            
            // --- NAYE FIELDS FRONTEND KO BHEJNE KE LIYE ---
            groupData.put("description", membership.getChatGroup().getDescription());
            groupData.put("permissions", membership.getChatGroup().getPermissions());
            groupData.put("profilePicture", membership.getChatGroup().getProfilePicture());
            // ----------------------------------------------
            
            groupData.put("isGroup", true); 
            groupData.put("lastMessage", "Tap to start chatting...");
            groupData.put("unreadCount", 0);
            return groupData;
        }).collect(Collectors.toList());
    }

    // GroupService.java mein aakhir mein 
    public List<Map<String, Object>> getGroupMembers(Long groupId) {
        List<GroupMember> members = groupMemberRepository.findByChatGroupId(groupId);
        return members.stream().map(m -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", m.getUser().getId());
            userMap.put("username", m.getUser().getUsername());
            userMap.put("profilePicture", m.getUser().getProfilePicture());
            userMap.put("online", m.getUser().isOnline());
            userMap.put("customStatusText", m.getUser().getCustomStatusText());
            
            // NAYA: Status ki baaki details bhi bhejni hongi
            userMap.put("statusState", m.getUser().getStatusState()); 
            userMap.put("customStatusColor", m.getUser().getCustomStatusColor()); 
            
            return userMap;
        }).collect(Collectors.toList());
    }

    // ==========================================
    // NAYA: ADD MEMBERS TO EXISTING GROUP LOGIC
    // ==========================================
    @Transactional
    public void addMembersToGroup(Long groupId, List<Long> userIds, Long addedById) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        User addedBy = userRepository.findById(addedById).orElse(null);
        String adderName = (addedBy != null) ? addedBy.getUsername() : "Someone";

        // Pehle se group me kon hai wo nikal lo
        List<GroupMember> existingMembers = groupMemberRepository.findByChatGroupId(groupId);
        List<Long> existingUserIds = existingMembers.stream().map(m -> m.getUser().getId()).collect(Collectors.toList());

        for (Long userId : userIds) {
            // Agar banda pehle se group me NAHI hai, tabhi add karo
            if (!existingUserIds.contains(userId)) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    GroupMember member = new GroupMember();
                    member.setChatGroup(group);
                    member.setUser(user);
                    groupMemberRepository.save(member);

                    // System Message: "Sanskriti added Priya."
                    Map<String, Object> sysPayload = new HashMap<>();
                    sysPayload.put("content", adderName + " added " + user.getUsername() + ".");
                    sysPayload.put("senderId", addedById);
                    sysPayload.put("senderName", "System");
                    sysPayload.put("roomId", "GROUP_" + groupId);
                    groupMessageService.saveAndBroadcastMessage(groupId, sysPayload);

                    // Naye user ko notification bhejo taaki uske app me group load ho jaye
                    Map<String, String> notification = new HashMap<>();
                    notification.put("type", "NEW_GROUP");
                    try {
                        messagingTemplate.convertAndSend("/topic/user/" + userId, notification);
                    } catch (Exception e) { }
                }
            }
        }
    }
}