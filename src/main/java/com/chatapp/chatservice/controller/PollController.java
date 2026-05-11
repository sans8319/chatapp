package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.entity.Poll;
import com.chatapp.chatservice.entity.PollOption;
import com.chatapp.chatservice.service.PollService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PollController {

    private final PollService pollService;
    private final com.chatapp.chatservice.controller.FileController fileController; // Existing file upload reuse

    @PostMapping("/create")
    public ResponseEntity<?> createPoll(
            @RequestParam("pollData") String pollDataJson,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            Poll poll = mapper.readValue(pollDataJson, Poll.class);
            List<PollOption> options = poll.getOptions();
            
            // Files handle karna (Agar hain toh)
            int fileIndex = 0;
            for (PollOption option : options) {
                // Hum frontend se track rakhenge ki kis option ki file hai
                if (option.getFileSize() != null && files != null && fileIndex < files.length) {
                    // Aapka purana FileController logic use karke file upload
                    MultipartFile currentFile = files[fileIndex++];
                    ResponseEntity<java.util.Map> uploadRes = (ResponseEntity) fileController.uploadFile(currentFile);
                    
                    if (uploadRes.getStatusCode().is2xxSuccessful()) {
                        option.setFileUrl((String) uploadRes.getBody().get("url"));
                        option.setFileName(currentFile.getOriginalFilename());
                        option.setFileType(currentFile.getContentType());
                    }
                }
            }

            Poll createdPoll = pollService.createPoll(poll, options);
            return ResponseEntity.ok(createdPoll);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Poll creation failed: " + e.getMessage());
        }
    }
}