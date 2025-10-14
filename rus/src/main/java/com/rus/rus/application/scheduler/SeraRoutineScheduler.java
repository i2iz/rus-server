package com.rus.rus.application.scheduler;

import com.rus.rus.domain.Routine;
import com.rus.rus.domain.RoutineSera;
import com.rus.rus.domain.UserProfile;
import com.rus.rus.infra.repository.RoutineRepository;
import com.rus.rus.infra.repository.RoutineSeraRepository;
import com.rus.rus.infra.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SeraRoutineScheduler {

  private final UserProfileRepository userProfileRepository;
  private final RoutineRepository routineRepository;
  private final RoutineSeraRepository routineSeraRepository;

  /**
   * 매주 월요일 자정에 모든 사용자에게 2개의 랜덤 루틴을 Sera 추천 루틴으로 할당합니다.
   * 기존에 할당된 추천 루틴은 모두 삭제됩니다.
   */
  @Scheduled(cron = "0 0 0 * * MON")
  @Transactional
  public void assignWeeklySeraRoutines() {
    System.out.println("Sera 주간 추천 루틴 할당을 시작합니다.");

    // 1. 현재 등록되어 있는 모든 사용자를 조회합니다.
    List<UserProfile> allUsers = userProfileRepository.findAll();
    if (allUsers.isEmpty()) {
      System.out.println("등록된 사용자가 없어 스케줄러를 종료합니다.");
      return;
    }

    // 2. routines 테이블에서 모든 루틴(템플릿)을 조회합니다.
    List<Routine> allRoutines = routineRepository.findAll();
    if (allRoutines.size() < 2) {
      System.out.println("할당할 루틴이 2개 미만이므로 스케줄러를 종료합니다.");
      return;
    }

    // 3. 각 사용자에 대해 작업을 수행합니다.
    for (UserProfile user : allUsers) {
      // 4. 월요일 자정이 되면 기존 미션을 제거합니다.
      routineSeraRepository.deleteAllInBatch(routineSeraRepository.findByUserProfile_Uid(user.getUid()));

      // 5. 전체 루틴 목록을 섞은 후 2개를 랜덤으로 선택합니다.
      Collections.shuffle(allRoutines);
      List<Routine> randomRoutines = allRoutines.subList(0, 2);

      // 6. 선택된 루틴을 사용자의 Sera 추천 루틴으로 저장합니다.
      for (Routine routine : randomRoutines) {
        RoutineSera seraRoutine = RoutineSera.builder()
            .userProfile(user)
            .category(routine.getCategory())
            .content(routine.getContent())
            .build();
        routineSeraRepository.save(seraRoutine);
      }
    }
    System.out.println(allUsers.size() + "명의 사용자에 대한 Sera 주간 추천 루틴 할당을 완료했습니다.");
  }
}