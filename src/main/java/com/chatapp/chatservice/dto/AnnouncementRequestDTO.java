package com.chatapp.chatservice.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnnouncementRequestDTO {
    private String title;
    private String category;
    private String message;
    private String targetedAudience;
    private List<String> targetDepartments;
    private List<Long> targetUsers;
    private String priority;
    private LocalDateTime expiryDate;
    private Long createdBy;
    private List<AttachmentInfo> attachments;
    
    @Data
    public static class AttachmentInfo {
        private String fileName;
        private String fileUrl;
        private String fileType;
        private Long fileSize;
    }
}