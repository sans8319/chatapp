package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    // Group history fetch karne ke liye
    List<GroupMessage> findByChatGroupIdOrderByTimestampAsc(Long groupId);
    

    // NAYA: Groups ka Media/Links laane ke liye
    @org.springframework.data.jpa.repository.Query("SELECT gm FROM GroupMessage gm WHERE gm.chatGroup.id = :groupId AND (gm.fileUrl IS NOT NULL OR gm.content LIKE '%http%') ORDER BY gm.timestamp DESC")
    List<GroupMessage> findMediaAndLinksByGroupId(@org.springframework.data.repository.query.Param("groupId") Long groupId);


    // 🛑 NAYA: Group delete hone par saare messages saaf karne ke liye
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM GroupMessage gm WHERE gm.chatGroup.id = :groupId")
    void deleteByGroupId(@org.springframework.data.repository.query.Param("groupId") Long groupId);
}