package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    
    @Query("SELECT DISTINCT a FROM Announcement a " +
       "LEFT JOIN a.targetDepartments td " +
       "LEFT JOIN a.targetUsers tu " +
       "WHERE a.targetedAudience = 'all' " +
       "OR (a.targetedAudience = 'department' AND td = :dept) " +
       "OR (a.targetedAudience = 'specific' AND tu = :userId) " +
       "OR a.createdBy = :userId " +  // ✅ YEH LINE ADD KARO - Creator ka announcement hamesha dikhega
       "ORDER BY a.createdAt DESC")
   List<Announcement> findVisibleAnnouncements(@Param("dept") String dept, @Param("userId") Long userId);

    // Naya repository method (agar tracking table banani hai)
    @Query("SELECT COUNT(av) > 0 FROM AnnouncementView av WHERE av.announcementId = :announcementId AND av.userId = :userId")
    boolean isAnnouncementOpenedByUser(@Param("announcementId") Long announcementId, @Param("userId") Long userId);


    // 🛑 NAYA: Expired announcements dhoondhne ke liye
    List<Announcement> findByExpiryDateBefore(java.time.LocalDateTime date);
}