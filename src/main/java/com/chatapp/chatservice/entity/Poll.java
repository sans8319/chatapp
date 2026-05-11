package com.chatapp.chatservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "polls")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Poll {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(length = 1000)
    private String description;

    // "single" ya "multiple"
    private String pollType; 

    // "all", "department", ya "specific"
    private String targetedAudience; 

    // "anonymous" ya "public"
    private String visibility; 

    // Agar audience 'department' hai, toh unki list
    @ElementCollection
    @CollectionTable(name = "poll_target_departments", joinColumns = @JoinColumn(name = "poll_id"))
    @Column(name = "department_name")
    private List<String> targetDepartments = new ArrayList<>();

    // Agar audience 'specific' hai, toh un users ki IDs
    @ElementCollection
    @CollectionTable(name = "poll_target_users", joinColumns = @JoinColumn(name = "poll_id"))
    @Column(name = "user_id")
    private List<Long> targetUsers = new ArrayList<>();

    private Long createdBy; // Jisne poll banaya
    
    private LocalDateTime createdAt;
    
    private boolean isActive = true;

    // Poll options ke sath connection
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollOption> options = new ArrayList<>();

    private LocalDateTime expiryDate;
}