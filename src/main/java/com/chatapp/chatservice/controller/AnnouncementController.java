package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.dto.AnnouncementRequestDTO;
import com.chatapp.chatservice.entity.Announcement;
import com.chatapp.chatservice.entity.AnnouncementAttachment;
import com.chatapp.chatservice.service.AnnouncementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final FileController fileController;
    private final ObjectMapper objectMapper;

    @PostMapping("/create")
    public ResponseEntity<?> createAnnouncement(
            @RequestParam("announcementData") String announcementDataJson,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        
        try {
            AnnouncementRequestDTO request = objectMapper.readValue(announcementDataJson, AnnouncementRequestDTO.class);
            
            List<AnnouncementAttachment> attachments = new ArrayList<>();
            int fileIndex = 0;
            
            if (files != null && request.getAttachments() != null) {
                for (AnnouncementRequestDTO.AttachmentInfo attInfo : request.getAttachments()) {
                    if (files.length > fileIndex) {
                        MultipartFile currentFile = files[fileIndex++];
                        
                        // ✅ FIXED: No cast needed
                        ResponseEntity<Map<String, String>> uploadRes = fileController.uploadFile(currentFile);
                        
                        if (uploadRes.getStatusCode().is2xxSuccessful() && uploadRes.getBody() != null) {
                            AnnouncementAttachment attachment = AnnouncementAttachment.builder()
                                    .fileName(currentFile.getOriginalFilename())
                                    .fileUrl(uploadRes.getBody().get("url"))
                                    .fileType(currentFile.getContentType())
                                    .fileSize(currentFile.getSize())
                                    .build();
                            attachments.add(attachment);
                        }
                    }
                }
            }
            
            Announcement created = announcementService.createAnnouncement(request, attachments);
            return ResponseEntity.ok(created);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Announcement creation failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Announcement>> getAnnouncementsForUser(@PathVariable Long userId) {
        try {
            List<Announcement> announcements = announcementService.getAnnouncementsForUser(userId);
            return ResponseEntity.ok(announcements);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{announcementId}")
    public ResponseEntity<?> deleteAnnouncement(
            @PathVariable Long announcementId,
            @RequestParam Long userId) {
        try {
            announcementService.deleteAnnouncement(announcementId, userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Announcement deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🛑 NAYA: Mark Announcement as Opened API
    @PostMapping("/{announcementId}/mark-opened")
    public ResponseEntity<?> markAsOpened(
            @PathVariable Long announcementId,
            @RequestParam Long userId) {
        try {
            announcementService.markAnnouncementAsOpened(announcementId, userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Marked as opened"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{announcementId}/update")
    public ResponseEntity<?> updateAnnouncement(
            @PathVariable Long announcementId,
            @RequestParam("announcementData") String announcementDataJson,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam Long userId) {
        try {
            AnnouncementRequestDTO request = objectMapper.readValue(announcementDataJson, AnnouncementRequestDTO.class);
            
            Announcement updated = announcementService.updateAnnouncement(announcementId, request, files, userId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Update failed: " + e.getMessage());
        }
    }

    // 🛑 NAYA: Viewers list fetch API
    @GetMapping("/{announcementId}/viewers")
    public ResponseEntity<?> getAnnouncementViewers(@PathVariable Long announcementId) {
        try {
            return ResponseEntity.ok(announcementService.getAnnouncementViewers(announcementId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}