package com.chatapp.chatservice.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MessageReceipt {
    private Long messageId;
    private String status; // "DELIVERED" ya "SEEN"
    private Long roomId;
}