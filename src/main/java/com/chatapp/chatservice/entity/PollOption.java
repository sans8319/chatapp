package com.chatapp.chatservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poll_options")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Option ka text (Ya fir file ka naam agar sirf file di hai)
    @Column(length = 500)
    private String text;

    // =====================================
    // MEDIA HANDLING KE LIYE FIELDS
    // =====================================
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;

    // Poll table ke sath mapping
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    @JsonIgnore // Infinite loop se bachane ke liye
    private Poll poll;

    @Transient
    private long voteCount; // Is option par kitne vote aaye hain

    @Transient
    private boolean isUserChoice; // Kya is user ne ye chuna tha
}