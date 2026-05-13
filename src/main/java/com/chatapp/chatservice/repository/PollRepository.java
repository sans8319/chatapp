package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // 🛑 NAYA: Missing import add kiya
import org.springframework.data.repository.query.Param; // 🛑 NAYA: Missing import add kiya
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
    
    List<Poll> findAllByIsActiveTrueAndExpiryDateAfterOrderByCreatedAtDesc(LocalDateTime now);

    @Query("SELECT DISTINCT p FROM Poll p " +
        "LEFT JOIN p.targetDepartments td " +
        "LEFT JOIN p.targetUsers tu " +
        "WHERE p.targetedAudience = 'all' " +
        "OR (p.targetedAudience = 'department' AND td = :dept) " +
        "OR (p.targetedAudience = 'specific' AND tu = :userId) " +
        "ORDER BY p.createdAt DESC")
    List<Poll> findVisiblePolls(@Param("dept") String dept, @Param("userId") Long userId);
}