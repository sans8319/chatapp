package com.chatapp.chatservice.repository;


import com.chatapp.chatservice.entity.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {}