package com.chatapp.chatservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_messages")
public class GroupMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;
    private Long senderId;
    private String senderName; 

    @ManyToOne
    @JoinColumn(name = "group_id")
    private ChatGroup chatGroup;

    private LocalDateTime timestamp = LocalDateTime.now();

    // =====================================
    // NAYA: Media Sharing Fields
    // =====================================
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;

    // =====================================
    // PURANE Getters and Setters
    // =====================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public ChatGroup getChatGroup() { return chatGroup; }
    public void setChatGroup(ChatGroup chatGroup) { this.chatGroup = chatGroup; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    // =====================================
    // NAYE Getters and Setters (Isi ki wajah se error thi)
    // =====================================
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }


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
    private boolean isDeleted = false;
    
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}