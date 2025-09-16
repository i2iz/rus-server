package com.rus.rus.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rus.rus.domain.RoutineChallenge;
import com.rus.rus.domain.idClass.RoutineChallengeId;

import java.util.Optional;

@Repository
public interface RoutineChallengeRepository extends JpaRepository<RoutineChallenge, RoutineChallengeId> {
    /**
     * 현재 활성화된 챌린지를 조회합니다.
     * 혹시 오류같은게 있어서 데이터가 여러개 들어있더라도, 첫 번째 데이터만 조회합니다.
     * @return Optional<RoutineChallenge>
     */
    Optional<RoutineChallenge> findFirstBy();
}
