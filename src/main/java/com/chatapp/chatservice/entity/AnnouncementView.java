package com.chatapp.chatservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcement_views")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AnnouncementView {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long announcementId;
    private Long userId;
    private LocalDateTime openedAt;
    
    @PrePersist
    protected void onCreate() {
        openedAt = LocalDateTime.now();
    }
}