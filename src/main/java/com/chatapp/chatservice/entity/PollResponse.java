package com.chatapp.chatservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty; // 🛑 1. NAYA IMPORT ADD KAREIN
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "poll_responses", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"poll_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PollResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "poll_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // 🛑 2. FIX: @JsonIgnore hata kar isse replace karein
    private Poll poll;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ElementCollection
    @CollectionTable(name = "poll_response_options", joinColumns = @JoinColumn(name = "response_id"))
    @Column(name = "option_id")
    private List<Long> selectedOptionIds;

    @Column(length = 500)
    private String comment;

    private LocalDateTime submittedAt;

    @Transient 
    private User user;

    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
    }
}