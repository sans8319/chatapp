package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    // Spring Data JPA apne aap saare basic functions (save, findById) de dega
    List<Message> findByChatRoomIdOrderByTimestampAsc(Long roomId);
}