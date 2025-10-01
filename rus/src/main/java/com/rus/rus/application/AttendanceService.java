package com.rus.rus.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.res.AttendanceCheckResponseDto;
import com.rus.rus.controller.dto.res.AttendanceResponseDto;
import com.rus.rus.domain.UserProfile;
import com.rus.rus.domain.WeeklyAttendance;
import com.rus.rus.infra.repository.UserProfileRepository;
import com.rus.rus.infra.repository.WeeklyAttendanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final WeeklyAttendanceRepository weeklyAttendanceRepository;
    private final UserProfileRepository userProfileRepository;
    private static final int WEEKLY_ATTENDANCE_BONUS_LUX = 100;

    /**
     * 특정 사용자의 주간 출석부 데이터를 조회합니다.
     *
     * @param uid 조회할 사용자의 고유 식별자
     * @return 사용자의 주간 출석 현황이 담긴 DTO
     */
    @Transactional(readOnly = true)
    public AttendanceResponseDto getAttendanceStatus(UUID uid) {
        // 1. Repository를 통해 사용자의 출석부 데이터를 조회합니다.
        WeeklyAttendance attendance = weeklyAttendanceRepository.findById(uid.toString())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자의 출석부 정보를 찾을 수 없습니다."));

        // 현재 주의 시작일(월요일)과 종료일(일요일)을 계산합니다
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // 2. 조회된 엔티티의 데이터를 DTO로 변환하여 반환합니다.
        // timestamp는 API 명세에 따라 현재 시간으로 설정합니다.
        return AttendanceResponseDto.builder()
                .timestamp(ZonedDateTime.now()) // LocalDateTime.now()를 ZonedDateTime.now()로 변경
                .mon(attendance.getMon())
                .tue(attendance.getTue())
                .wed(attendance.getWed())
                .thu(attendance.getThu())
                .fri(attendance.getFri())
                .sat(attendance.getSat())
                .sun(attendance.getSun())
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    /**
     * 오늘의 요일에 해당하는 출석을 체크하고, 조건 충족 시 보너스를 지급합니다.
     *
     * @param uid 출석 체크할 사용자의 고유 식별자
     * @return 주간 개근 완료 여부가 담긴 DTO
     */
    @Transactional
    public AttendanceCheckResponseDto checkAttendance(UUID uid) {
        // 1. Repository를 통해 사용자의 출석부 데이터를 조회합니다.
        String userId = uid.toString();
        WeeklyAttendance attendance = weeklyAttendanceRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자의 출석부 정보를 찾을 수 없습니다."));

        // 2. 오늘의 요일을 가져옵니다.
        DayOfWeek today = LocalDate.now().getDayOfWeek();

        // 3. 오늘의 요일에 해당하는 필드를 true로 설정합니다.
        boolean isAlreadyCheckedIn = isAlreadyCheckedIn(attendance, today);

        if (!isAlreadyCheckedIn) {
            updateAttendanceForToday(attendance, today);
        }

        // 4. 주간 개근 여부를 확인하고 보너스를 지급합니다.
        boolean isCompleted = false;
        if (today == DayOfWeek.SUNDAY && isWeeklyAttendancePerfect(attendance) && !isAlreadyCheckedIn) {
            UserProfile userProfile = userProfileRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."));

            userProfile.setLux(userProfile.getLux() + WEEKLY_ATTENDANCE_BONUS_LUX);
            isCompleted = true;
        }

        // 5. 최종 응답 DTO를 빌드하여 반환합니다.
        return AttendanceCheckResponseDto.builder()
                .completed(isCompleted)
                .build();
    }

    /**
     * 요일에 따라 출석부의 해당 필드를 true로 업데이트하는 헬퍼 메서드
     */
    private void updateAttendanceForToday(WeeklyAttendance attendance, DayOfWeek day) {
        switch (day) {
            case MONDAY:
                attendance.setMon(true);
                break;
            case TUESDAY:
                attendance.setTue(true);
                break;
            case WEDNESDAY:
                attendance.setWed(true);
                break;
            case THURSDAY:
                attendance.setThu(true);
                break;
            case FRIDAY:
                attendance.setFri(true);
                break;
            case SATURDAY:
                attendance.setSat(true);
                break;
            case SUNDAY:
                attendance.setSun(true);
                break;
        }
    }

    /**
     * 월요일부터 일요일까지 모두 출석했는지 확인하는 헬퍼 메서드
     */
    private boolean isWeeklyAttendancePerfect(WeeklyAttendance attendance) {
        // Boolean 객체는 null일 수 있으므로, false와 명시적으로 비교합니다.
        return Boolean.TRUE.equals(attendance.getMon()) &&
                Boolean.TRUE.equals(attendance.getTue()) &&
                Boolean.TRUE.equals(attendance.getWed()) &&
                Boolean.TRUE.equals(attendance.getThu()) &&
                Boolean.TRUE.equals(attendance.getFri()) &&
                Boolean.TRUE.equals(attendance.getSat()) &&
                Boolean.TRUE.equals(attendance.getSun());
    }

    /**
     * 특정 요일의 출석 여부를 반환하는 헬퍼 메소드
     */
    private boolean isAlreadyCheckedIn(WeeklyAttendance attendance, DayOfWeek day) {
        return switch (day) {
            case MONDAY -> Boolean.TRUE.equals(attendance.getMon());
            case TUESDAY -> Boolean.TRUE.equals(attendance.getTue());
            case WEDNESDAY -> Boolean.TRUE.equals(attendance.getWed());
            case THURSDAY -> Boolean.TRUE.equals(attendance.getThu());
            case FRIDAY -> Boolean.TRUE.equals(attendance.getFri());
            case SATURDAY -> Boolean.TRUE.equals(attendance.getSat());
            case SUNDAY -> Boolean.TRUE.equals(attendance.getSun());
        };
    }
}
