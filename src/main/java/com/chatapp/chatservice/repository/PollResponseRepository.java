package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.PollResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PollResponseRepository extends JpaRepository<PollResponse, Long> {
    // 🛑 NAYA: Check karne ke liye ki user pehle vote kar chuka hai ya nahi
    boolean existsByPollIdAndUserId(Long pollId, Long userId);
    Optional<PollResponse> findByPollIdAndUserId(Long pollId, Long userId);

    // 🛑 NAYA: Ek specific option par kitne logo ne vote kiya hai wo count karne ke liye
    @Query("SELECT COUNT(pr) FROM PollResponse pr JOIN pr.selectedOptionIds optId WHERE optId = :optionId")
    long countVotesByOptionId(@Param("optionId") Long optionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PollResponse pr WHERE pr.poll.id = :pollId")
    void deleteByPollId(@Param("pollId") Long pollId);
}