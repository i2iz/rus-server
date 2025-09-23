package com.rus.rus.infra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rus.rus.domain.UserRoutine;

public interface UserRoutineRepository extends JpaRepository<UserRoutine, Integer> {
    List<UserRoutine> findByUserProfileUid(String uid);
    List<UserRoutine> findByUserProfile_Uid(String uid);
}
