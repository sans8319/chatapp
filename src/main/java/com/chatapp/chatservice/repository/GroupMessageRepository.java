package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    // Group history fetch karne ke liye
    List<GroupMessage> findByChatGroupIdOrderByTimestampAsc(Long groupId);
}