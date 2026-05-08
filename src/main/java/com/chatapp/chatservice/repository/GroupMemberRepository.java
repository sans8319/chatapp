package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// Yahan <GroupMember, Long> hona zaroori hai!
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    long countByChatGroupId(Long groupId);
    List<GroupMember> findByUserId(Long userId);
    List<GroupMember> findByChatGroupId(Long groupId);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM GroupMember gm WHERE gm.chatGroup.id = :groupId")
    void deleteByGroupId(@org.springframework.data.repository.query.Param("groupId") Long groupId);
}