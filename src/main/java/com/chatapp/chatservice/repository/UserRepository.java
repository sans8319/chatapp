package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    // Search users by username (for starting new chats)
    List<User> findByUsernameContainingIgnoreCase(String username);
}