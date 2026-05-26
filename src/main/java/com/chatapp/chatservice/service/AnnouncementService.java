package com.chatapp.chatservice.service;

import com.chatapp.chatservice.dto.AnnouncementRequestDTO;
import com.chatapp.chatservice.entity.Announcement;
import com.chatapp.chatservice.entity.AnnouncementAttachment;
import com.chatapp.chatservice.entity.User;
import com.chatapp.chatservice.repository.AnnouncementRepository;
import com.chatapp.chatservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chatapp.chatservice.repository.AnnouncementViewRepository;
import com.chatapp.chatservice.entity.AnnouncementView;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.multipart.MultipartFile;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AnnouncementViewRepository announcementViewRepository;
    @Transactional
    public Announcement createAnnouncement(AnnouncementRequestDTO request, List<AnnouncementAttachment> attachments) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setCategory(request.getCategory());
        announcement.setMessage(request.getMessage());
        announcement.setTargetedAudience(request.getTargetedAudience());
        announcement.setTargetDepartments(request.getTargetDepartments() != null ? request.getTargetDepartments() : List.of());
        announcement.setTargetUsers(request.getTargetUsers() != null ? request.getTargetUsers() : List.of());
        announcement.setPriority(request.getPriority());
        announcement.setExpiryDate(request.getExpiryDate());
        announcement.setCreatedBy(request.getCreatedBy());
        
        if (attachments != null && !attachments.isEmpty()) {
            attachments.forEach(att -> att.setAnnouncement(announcement));
            announcement.setAttachments(attachments);
        }
        
        Announcement saved = announcementRepository.save(announcement);
        
        // Send real-time notification
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_ANNOUNCEMENT");
            notification.put("announcementId", saved.getId());
            messagingTemplate.convertAndSend("/topic/public/updates", notification);
        } catch (Exception e) {
            System.err.println("Announcement notification failed: " + e.getMessage());
        }
        
        return saved;
    }
    
    public List<Announcement> getAnnouncementsForUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        String dept = user.getDepartment() != null ? user.getDepartment() : "";
        List<Announcement> announcements = announcementRepository.findVisibleAnnouncements(dept, userId);
        
        // ✅ Set createdByUsername AND createdByProfilePicture for each
        announcements.forEach(a -> {
            userRepository.findById(a.getCreatedBy()).ifPresent(u -> {
                a.setCreatedByUsername(u.getUsername());
                a.setCreatedByProfilePicture(u.getProfilePicture()); // ✅ ADD THIS LINE
            });

            boolean isOpened = announcementViewRepository.isAnnouncementOpenedByUser(a.getId(), userId);
             a.setOpenedByUser(isOpened);
        });
        
        return announcements;
    }

    @Transactional
    public void deleteAnnouncement(Long announcementId, Long userId) {
        Announcement announcement = announcementRepository.findById(announcementId)
            .orElseThrow(() -> new RuntimeException("Announcement not found"));
        
        // ✅ Sirf creator hi delete kar sakta hai
        if (!announcement.getCreatedBy().equals(userId)) {
            throw new RuntimeException("You can only delete your own announcements");
        }
        
        // ✅ Delete attachments first (due to foreign key)
        if (announcement.getAttachments() != null) {
            announcement.getAttachments().clear();
        }
        
        // ✅ Delete announcement
        announcementRepository.delete(announcement);
        
        // ✅ Send real-time notification to everyone
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "ANNOUNCEMENT_DELETED");
            notification.put("announcementId", announcementId);
            notification.put("deletedBy", userId);
            messagingTemplate.convertAndSend("/topic/public/updates", notification);
        } catch (Exception e) {
            System.err.println("Delete notification failed: " + e.getMessage());
        }
    }

    @Transactional
    public void markAnnouncementAsOpened(Long announcementId, Long userId) {
        // Sirf recent announcements ke liye (jo user ne nahi banaye)
        Announcement announcement = announcementRepository.findById(announcementId).orElseThrow();
        
        // Agar khud create kiya hai toh opened mark nahi karna
        if (announcement.getCreatedBy().equals(userId)) {
            return;
        }
        
        // Check if already exists
        boolean alreadyOpened = announcementViewRepository.isAnnouncementOpenedByUser(announcementId, userId);
        if (!alreadyOpened) {
            AnnouncementView view = AnnouncementView.builder()
                .announcementId(announcementId)
                .userId(userId)
                .openedAt(LocalDateTime.now())
                .build();
            announcementViewRepository.save(view);
            try {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "ANNOUNCEMENT_VIEWED");
                notification.put("announcementId", announcementId);
                messagingTemplate.convertAndSend("/topic/public/updates", notification);
            } catch (Exception e) {
                System.err.println("View notification failed: " + e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") 
    @Transactional
    public void removeExpiredAnnouncementsAutomatically() {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> expiredAnnouncements = announcementRepository.findByExpiryDateBefore(now);
        
        if (!expiredAnnouncements.isEmpty()) {
            // 1. Delete all expired announcements from database
            announcementRepository.deleteAll(expiredAnnouncements);
            System.out.println("🗑️ Automatically deleted " + expiredAnnouncements.size() + " expired announcements.");
            
            // 2. Real-time update to all connected users (agar koi us waqt app chala raha ho)
            try {
                messagingTemplate.convertAndSend("/topic/public/updates", 
                    Map.of("type", "EXPIRED_ANNOUNCEMENTS_CLEARED"));
            } catch (Exception e) {
                System.err.println("Notification failed: " + e.getMessage());
            }
        }
    }

    @Transactional
    public Announcement updateAnnouncement(Long announcementId, AnnouncementRequestDTO request, MultipartFile[] files, Long userId) {
        Announcement existing = announcementRepository.findById(announcementId)
            .orElseThrow(() -> new RuntimeException("Announcement not found"));
        
        // ✅ Only creator can edit
        if (!existing.getCreatedBy().equals(userId)) {
            throw new RuntimeException("You can only edit your own announcements");
        }
        
        // Update editable fields only (audience cannot be changed)
        existing.setTitle(request.getTitle());
        existing.setCategory(request.getCategory());
        existing.setMessage(request.getMessage());
        existing.setPriority(request.getPriority());
        existing.setExpiryDate(request.getExpiryDate());
        
        // ✅ Handle new attachments (if any new files uploaded)
        if (files != null && files.length > 0 && request.getAttachments() != null) {
            List<AnnouncementAttachment> newAttachments = new ArrayList<>();
            int fileIndex = 0;
            
            for (AnnouncementRequestDTO.AttachmentInfo attInfo : request.getAttachments()) {
                if (files.length > fileIndex) {
                    MultipartFile currentFile = files[fileIndex++];
                    
                    // You'll need FileController instance or copy upload logic
                    // For now, just save file info
                    AnnouncementAttachment attachment = AnnouncementAttachment.builder()
                            .fileName(currentFile.getOriginalFilename())
                            .fileUrl("/uploads/" + currentFile.getOriginalFilename()) // Update with actual upload logic
                            .fileType(currentFile.getContentType())
                            .fileSize(currentFile.getSize())
                            .announcement(existing)
                            .build();
                    newAttachments.add(attachment);
                }
            }
            
            // Append new attachments to existing ones
            if (existing.getAttachments() == null) {
                existing.setAttachments(new ArrayList<>());
            }
            existing.getAttachments().addAll(newAttachments);
        }
        
        Announcement saved = announcementRepository.save(existing);
        
        // Send real-time notification
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "ANNOUNCEMENT_UPDATED");
            notification.put("announcementId", announcementId);
            messagingTemplate.convertAndSend("/topic/public/updates", notification);
        } catch (Exception e) {
            System.err.println("Update notification failed: " + e.getMessage());
        }
        
        return saved;
    }

    // 🛑 NAYA: Get list of users who viewed
    public List<Map<String, Object>> getAnnouncementViewers(Long announcementId) {
        List<AnnouncementView> views = announcementViewRepository.findByAnnouncementIdOrderByOpenedAtDesc(announcementId);
        return views.stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("openedAt", v.getOpenedAt());
            
            userRepository.findById(v.getUserId()).ifPresent(u -> {
                map.put("username", u.getUsername());
                map.put("profilePicture", u.getProfilePicture());
            });
            return map;
        }).collect(Collectors.toList());
    }
}