package com.rus.rus.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.rus.rus.controller.dto.RoutineStatisticsDto;
import com.rus.rus.controller.dto.res.StatisticsResponseDto;
import com.rus.rus.domain.UserAttainment;
import com.rus.rus.domain.UserRoutine;
import com.rus.rus.infra.repository.UserAttainmentRepository;
import com.rus.rus.infra.repository.UserRoutineRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserRoutineRepository userRoutineRepository;
    private final UserAttainmentRepository userAttainmentRepository;

    /**
     * startDay~endDay 동안의 사용자의 루틴 달성 통계를 반환합니다
     * @param uid 사용자 고유 식별자
     * @param startDay 조회 시작일
     * @param endDay 조회 종료일
     * @return
     */
    @Transactional
    public StatisticsResponseDto getUserRoutineStatistics(UUID uid, LocalDate startDay, LocalDate endDay) {

      String userId = uid.toString();

      // 1. 데이터 조회
      List<UserRoutine> userRoutines = userRoutineRepository.findByUserProfileUid(userId);
      List<UserAttainment> attainmentsInPeriod = userAttainmentRepository.findByUserProfileUidAndTimestampBetween(
              userId, startDay.atStartOfDay(), endDay.atTime(LocalTime.MAX)
      );

      // 2. 데이터 가공 (최적화)
      Map<Integer, Set<LocalDate>> attainmentMap = attainmentsInPeriod.stream()
            .collect(Collectors.groupingBy(
                    attainment -> attainment.getUserRoutine().getId(),
                    Collectors.mapping(attainment -> attainment.getTimestamp().toLocalDate(), Collectors.toSet())
            ));

      // 3. 응답 데이터 조립
      List<RoutineStatisticsDto> routineStatisticsList = userRoutines.stream().map(routine -> {
          Map<String, Boolean> dailyAttainments = new LinkedHashMap<>();
          Set<LocalDate> routineAttainmentDates = attainmentMap.getOrDefault(routine.getId(), Collections.emptySet());
          for (LocalDate date = startDay; !date.isAfter(endDay); date = date.plusDays(1)) {
              dailyAttainments.put(date.toString(), routineAttainmentDates.contains(date));
          }
          return RoutineStatisticsDto.builder()
                  .id(routine.getId())
                  .content(routine.getContent())
                  .attainments(dailyAttainments)
                  .build();
      }).collect(Collectors.toList());

      // 4. 최종 반환
      return StatisticsResponseDto.builder()
              .startDay(startDay)
              .endDay(endDay)
              .routines(routineStatisticsList)
              .build();
    }
}
