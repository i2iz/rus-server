package com.rus.rus.infra.repository;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rus.rus.domain.UserSleep;

@Repository
public interface UserSleepRepository extends JpaRepository<UserSleep, Long> {

    /**
     * 특정 사용자의 특정 날짜에 해당하는 가장 최신 수면 기록을 조회합니다.
     *
     * @param uid  사용자 고유 식별자
     * @param date 조회할 날짜
     * @return Optional<UserSleep>
     */
    Optional<UserSleep> findTopByUserProfileUidAndTimestampOrderBySleepLogIdDesc(String uid, LocalDate date);

    /**
     * 특정 사용자의 특정 기간 내 모든 수면 기록을 조회합니다.
     *
     * @param uid       사용자 고유 식별자
     * @param startDate 조회 시작일
     * @param endDate   조회 종료일
     * @return List<UserSleep>
     */
    List<UserSleep> findAllByUserProfileUidAndTimestampBetween(String uid, LocalDate startDate, LocalDate endDate);
}