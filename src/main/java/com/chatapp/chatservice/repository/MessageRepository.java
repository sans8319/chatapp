package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    // Spring Data JPA apne aap saare basic functions (save, findById) de dega
    List<Message> findByChatRoomIdOrderByTimestampAsc(Long roomId);

    // NAYA: 1-on-1 Chats ka Media/Links laane ke liye
    @org.springframework.data.jpa.repository.Query("SELECT m FROM Message m WHERE m.chatRoom.id = :roomId AND (m.fileUrl IS NOT NULL OR m.content LIKE '%http%') ORDER BY m.timestamp DESC")
    List<Message> findMediaAndLinksByRoomId(@org.springframework.data.repository.query.Param("roomId") Long roomId);
}