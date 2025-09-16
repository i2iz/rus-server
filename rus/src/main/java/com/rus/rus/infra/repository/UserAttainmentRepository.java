package com.rus.rus.infra.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rus.rus.domain.UserAttainment;


public interface UserAttainmentRepository extends JpaRepository<UserAttainment, Long> {
    List<UserAttainment> findByUserProfileUidAndTimestampBetween(String uid, LocalDateTime start, LocalDateTime end);
}
