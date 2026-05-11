package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.Poll;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findAllByIsActiveTrueAndExpiryDateAfterOrderByCreatedAtDesc(LocalDateTime now);
}