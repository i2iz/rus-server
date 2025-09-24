package com.rus.rus.application.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rus.rus.infra.repository.WeeklyAttendanceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final WeeklyAttendanceRepository weeklyAttendanceRepository;

    /**
     * 매주 월요일 자정에 모든 사용자의 주간 출석부 데이터를 초기화합니다.
     */
    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void resetWeeklyAttendance() {
        System.out.println("주간 출석부 데이터를 초기화합니다.");
        weeklyAttendanceRepository.resetAllAttendance();
        System.out.println("주간 출석부 초기화 완료.");
    }
}
