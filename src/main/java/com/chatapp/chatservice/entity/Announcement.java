package com.chatapp.chatservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "announcements")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Announcement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(nullable = false)
    private String category;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    // "all", "department", "specific"
    private String targetedAudience;
    
    @ElementCollection
    @CollectionTable(name = "announcement_target_departments", joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "department_name")
    private List<String> targetDepartments = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "announcement_target_users", joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "user_id")
    private List<Long> targetUsers = new ArrayList<>();
    
    private String priority; // Low, Normal, High, Critical
    
    private LocalDateTime expiryDate;
    
    private LocalDateTime createdAt;
    
    private Long createdBy;
    
    @Transient
    private String createdByUsername;
    
    // Attachments (JSON ya separate table)
    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnnouncementAttachment> attachments = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Transient
    private String createdByProfilePicture;

    // Getter and Setter
    public String getCreatedByProfilePicture() { return createdByProfilePicture; }
    public void setCreatedByProfilePicture(String createdByProfilePicture) { 
        this.createdByProfilePicture = createdByProfilePicture; 
    }

    @Transient
    private boolean isOpenedByUser; // Kya current user ne ye announcement open kiya hai

    public boolean isOpenedByUser() { return isOpenedByUser; }
    public void setOpenedByUser(boolean openedByUser) { isOpenedByUser = openedByUser; }
}