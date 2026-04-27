package com.chatapp.chatservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileDTO {
    private String name;
    private String email;
    private String about;
    private String department;
    private String designation;
    private String location;
    private String phone;
    private String profilePicture;
    private String statusState;
    private String customStatusText;
    private String customStatusColor;
}