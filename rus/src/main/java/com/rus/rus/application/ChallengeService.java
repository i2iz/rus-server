package com.rus.rus.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.CategoryDto;
import com.rus.rus.controller.dto.res.ChallengeResponseDto;
import com.rus.rus.domain.RoutineChallenge;
import com.rus.rus.infra.repository.ChallengeUserRepository;
import com.rus.rus.infra.repository.RoutineChallengeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeService {

  private final RoutineChallengeRepository routineChallengeRepository;
  private final ChallengeUserRepository challengeUserRepository;

  /**
     * 현재 챌린지 미션의 정보와 오늘 완료한 참여자 수를 조회합니다.
     *
     * @return 챌린지 정보가 담긴 DTO
     */
    @Transactional(readOnly = true)
    public ChallengeResponseDto getChallengeInfo() {
      // 1. 현재 진행 중인 챌린지 미션을 조회합니다.
      RoutineChallenge currentChallenge = routineChallengeRepository.findFirstBy()
              .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "챌린지 미션 정보가 존재하지 않습니다"));

      // 2. 오늘 챌린지를 완료한(check=true) 참여자 수를 조회합니다.
      // challenge_users 테이블은 매일 초기화되므로 날짜 조건 없이 count합니다.
      long participantCount = challengeUserRepository.countByCheckIsTrue();

      // 3. 응답 DTO를 조립합니다.
      CategoryDto categoryDto = CategoryDto.builder()
              .categoryId(currentChallenge.getCategory().getCategoryId())
              .value(currentChallenge.getCategory().getValue())
              .build();

      return ChallengeResponseDto.builder()
              .category(categoryDto)
              .content(currentChallenge.getContent())
              .participant(participantCount)
              .build();
  }
}
