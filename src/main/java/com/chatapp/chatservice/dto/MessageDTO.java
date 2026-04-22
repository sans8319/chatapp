package com.chatapp.chatservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MessageDTO {
    private Long id;
    private String content;
    private String senderUsername; // Frontend ko pura User object nahi, sirf naam chahiye
    private Long roomId;
    private LocalDateTime timestamp;
    private Long senderId;
    
    // Status Ticks ke liye
    private boolean delivered;
    private boolean seen;
}