package com.rus.rus.infra.repository;

import com.rus.rus.domain.RecoveryMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecoveryMissionRepository extends JpaRepository<RecoveryMission, Long> {
    Optional<RecoveryMission> findByUid(String uid);
}
