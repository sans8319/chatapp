package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.entity.Poll;
import com.chatapp.chatservice.entity.PollOption;
import com.chatapp.chatservice.entity.PollResponse;
import com.chatapp.chatservice.service.PollService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; 
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate; 
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PollController {

    private final PollService pollService;
    private final com.chatapp.chatservice.controller.FileController fileController; 
    private final SimpMessagingTemplate messagingTemplate; 

    @PostMapping("/create")
    public ResponseEntity<?> createPoll(
            @RequestParam("pollData") String pollDataJson,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule()); 
            Poll poll = mapper.readValue(pollDataJson, Poll.class);
            List<PollOption> options = poll.getOptions();
            
            int fileIndex = 0;
            if (options != null) {
                for (PollOption option : options) {
                    if (option.getFileSize() != null && files != null && fileIndex < files.length) {
                        MultipartFile currentFile = files[fileIndex++];
                        ResponseEntity<java.util.Map> uploadRes = (ResponseEntity) fileController.uploadFile(currentFile);
                        
                        if (uploadRes.getStatusCode().is2xxSuccessful()) {
                            option.setFileUrl((String) uploadRes.getBody().get("url"));
                            option.setFileName(currentFile.getOriginalFilename());
                            option.setFileType(currentFile.getContentType());
                        }
                    }
                }
            }

            Poll createdPoll = pollService.createPoll(poll, options);
            try {
                messagingTemplate.convertAndSend("/topic/public/updates", Map.of("type", "NEW_POLL"));
            } catch (Exception e) {
                System.err.println("Poll notification failed: " + e.getMessage());
            }
            return ResponseEntity.ok(createdPoll);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Poll creation failed: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Poll>> getPollsForUser(@PathVariable Long userId) {
        try {
            List<Poll> polls = pollService.getPollsForUser(userId);
            return ResponseEntity.ok(polls);
        } catch (Exception e) {
            e.printStackTrace(); // 🛑 FIX: Agar koi error aata hai to terminal me dikhega
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/vote")
    public ResponseEntity<?> submitVote(@RequestBody PollResponse pollResponse) {
        try {
            PollResponse savedResponse = pollService.saveVote(pollResponse);
            try {
                messagingTemplate.convertAndSend("/topic/public/updates", Map.of("type", "VOTE_SUBMITTED"));
            } catch (Exception e) {
                System.err.println("Vote notification failed: " + e.getMessage());
            }
            return ResponseEntity.ok(savedResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An error occurred while voting");
        }
    }
}