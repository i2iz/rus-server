package com.rus.rus.infra.repository;

import com.rus.rus.domain.RecoveryMission;
import com.rus.rus.domain.RecoveryMissionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecoveryMissionItemRepository extends JpaRepository<RecoveryMissionItem, Long> {
    List<RecoveryMissionItem> findByRecoveryMission(RecoveryMission recoveryMission);
    Optional<RecoveryMissionItem> findByRecoveryMissionAndRoutine_Rid(RecoveryMission recoveryMission, Integer rid);
}

