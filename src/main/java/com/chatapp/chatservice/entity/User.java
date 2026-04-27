package com.chatapp.chatservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String profilePicture;
    private boolean online;

    // =====================================
    // NAYE PROFILE FIELDS
    // =====================================
    @Column(name = "about")
    private String about;

    @Column(name = "department")
    private String department;

    @Column(name = "designation")
    private String designation;

    @Column(name = "work_location")
    private String location;

    @Column(name = "phone_number")
    private String phone;

    @Column(name = "status_state")
    private String statusState;

    @Column(name = "custom_status_text")
    private String customStatusText;

    @Column(name = "custom_status_color")
    private String customStatusColor;

    @Column(name = "country_code")
    private String countryCode;
}