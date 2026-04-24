package com.chatapp.chatservice.dto;

import java.util.List;

public class GroupCreateRequest {
    private String name;
    private List<Long> memberIds; 
    
    // --- NAYE FIELDS ---
    private String description; 
    private String permissions; 
    private String profilePicture; 

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Long> getMemberIds() { return memberIds; }
    public void setMemberIds(List<Long> memberIds) { this.memberIds = memberIds; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
}