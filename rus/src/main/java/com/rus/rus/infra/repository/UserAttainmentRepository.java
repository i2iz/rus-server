package com.rus.rus.infra.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rus.rus.domain.UserAttainment;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAttainmentRepository extends JpaRepository<UserAttainment, Long> {
    List<UserAttainment> findByUserProfileUidAndTimestampBetween(String uid, LocalDateTime start, LocalDateTime end);
    List<UserAttainment> findByUserProfile_UidAndTimestampBetween(
            String uid,
            LocalDateTime start,
            LocalDateTime end
    );

    boolean existsByUserRoutine_IdAndTimestampBetween(
            Integer id,
            LocalDateTime start,
            LocalDateTime end
    );

    boolean existsByUserProfile_UidAndUserRoutine_IdAndTimestampBetween(
            String uid,
            Integer id,
            LocalDateTime start,
            LocalDateTime end
    );

    void deleteByUserProfile_UidAndUserRoutine_IdAndTimestampBetween(
            String uid,
            Integer id,
            LocalDateTime start,
            LocalDateTime end
    );
}
