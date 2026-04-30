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
}