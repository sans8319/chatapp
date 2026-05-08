package com.chatapp.chatservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime timestamp;
    
    private boolean seen; // Aapke paas ye pehle se hai
    
    // NAYA: Delivered track karne ke liye
    private boolean delivered = false;
    
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;

    // NAYA: Jisne chat clear ki, uski ID isme save hogi (e.g., ",1,2,")
    @jakarta.persistence.Column(columnDefinition = "varchar(255) default ''")
    private String clearedBy = "";
    public String getClearedBy() {
    return clearedBy;
    }

    public void setClearedBy(String clearedBy) {
        this.clearedBy = clearedBy;
    }

    @Column(name = "is_deleted")
    private Boolean isDeleted = false; // 🛑 NAYA: 'boolean' ko 'Boolean' (Capital B) kar diya

    public boolean isDeleted() {
        return this.isDeleted != null ? this.isDeleted : false;
    }
    public void setDeleted(boolean deleted) {
        this.isDeleted = deleted;
    }

    
    @Column(name = "reply_to_id")
    private Long replyToId;
    
    @Column(name = "reply_to_name")
    private String replyToName;
    
    @Column(name = "reply_to_content")
    private String replyToContent;
    
    @Column(name = "reply_to_file_url")
    private String replyToFileUrl;

   
    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    public boolean isPinned() { return this.isPinned != null ? this.isPinned : false; }
    public void setPinned(boolean pinned) { this.isPinned = pinned; }
}