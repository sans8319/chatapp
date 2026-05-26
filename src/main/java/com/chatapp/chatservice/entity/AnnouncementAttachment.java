package com.chatapp.chatservice.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore; // ✅ Ye naya import add karein

@Entity
@Table(name = "announcement_attachments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AnnouncementAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id")
    @JsonIgnore // ✅ BAS YE EK ANNOTATION ADD KARNA HAI
    private Announcement announcement;
    
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
}