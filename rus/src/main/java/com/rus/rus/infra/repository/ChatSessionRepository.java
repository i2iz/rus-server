package com.rus.rus.infra.repository;

import com.rus.rus.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findBySessionIdAndUserProfile_Uid(String sessionId, String uid);
    List<ChatSession> findByUserProfile_UidOrderByUpdatedAtDesc(String uid);
}