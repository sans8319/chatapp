package com.chatapp.chatservice.dto;

import java.util.List;

public class GroupCreateRequest {
    private String name;
    private List<Long> memberIds; // Jin users ko add karna hai unki IDs

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Long> getMemberIds() { return memberIds; }
    public void setMemberIds(List<Long> memberIds) { this.memberIds = memberIds; }
}
