package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.AnnouncementView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnnouncementViewRepository extends JpaRepository<AnnouncementView, Long> {
    
    @Query("SELECT COUNT(av) > 0 FROM AnnouncementView av WHERE av.announcementId = :announcementId AND av.userId = :userId")
    boolean isAnnouncementOpenedByUser(Long announcementId, Long userId);

    List<AnnouncementView> findByAnnouncementIdOrderByOpenedAtDesc(Long announcementId);
}