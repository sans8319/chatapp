package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.PollComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PollCommentRepository extends JpaRepository<PollComment, Long> {
    // Naye comments pehle aayein isliye Descending order
    List<PollComment> findByPollIdOrderByCreatedAtDesc(Long pollId);
}