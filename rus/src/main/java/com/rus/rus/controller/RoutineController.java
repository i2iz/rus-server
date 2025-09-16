package com.rus.rus.controller;

import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rus.rus.application.ChallengeService;
import com.rus.rus.application.RoutineService;
import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.res.AllCollectionsResponseDto;
import com.rus.rus.controller.dto.res.AllRoutinesResponseDto;
import com.rus.rus.controller.dto.res.ChallengeResponseDto;
import com.rus.rus.controller.dto.res.RecommendResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/routine")
public class RoutineController {

    /**
     * 루틴 추천/생성 시 허용된 카테고리 목록
     */
    private static final List < String > ALLOWED_CATEGORIES =
        Arrays.asList("수면", "운동", "영양소", "햇빛", "사회적유대감");

    private final RoutineService routineService;
    private final ChallengeService challengeService;

    /**
     * 추천 루틴 생성
     * - 하나 이상의 카테고리를 지정하면 해당 카테고리에 높은 가중치를 부여하여 10개의 루틴을 추천해 줍니다.
     * @param categories 가중치를 부여할 카테고리 정보
     * @return 10개의 추천 루틴 생성
     */
    @GetMapping("/recommend")
    public ResponseEntity < RecommendResponseDto > getRecommendedRoutines(
        @RequestParam("category") List < String > categories
    ) {
        // 유효성 검증: 카테고리 파라미터가 비어있는지 확인합니다.
        if (categories == null || categories.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "하나 이상의 카테고리를 지정해야 합니다.");
        }

        // 유효성 검증: 요청된 각 카테고리 이름이 유효한 카테고리 인지 확인합니다
        for (String categoryName: categories) {
            if (!ALLOWED_CATEGORIES.contains(categoryName)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "유효하지 않은 카테고리 이름입니다: " + categoryName);
            }
        }

        // 서비스 로직을 호출하여 추천 루틴 데이터를 가져옵니다.
        RecommendResponseDto responseDto = routineService.getRecommendedRoutines(categories);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 모든 루틴 정보 반환
     * - routines 테이블에 정의된 모든 루틴에 대한 정보를 반환합니다.
     * @return
     */
    @GetMapping
    public ResponseEntity < AllRoutinesResponseDto > getAllRoutines() {
        AllRoutinesResponseDto responseDto = routineService.getAllRoutines();
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 챌린지 미션 조회
     * - 이번 달의 챌린지 미션 내용과, 오늘 날짜 기준 챌린지를 완료한 참여자 수를 반환합니다.
     * @return
     */
    @GetMapping("/challenge")
    public ResponseEntity < ChallengeResponseDto > getChallengeInfo() {
        // 서비스 로직을 호출하여 챌린지 정보를 가져옵니다.
        ChallengeResponseDto responseDto = challengeService.getChallengeInfo();
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Sera의 추천 루틴 모음 반환
     * - 컬렉션 테이블의 모든 정보와 각 컬렉션에 포함된 루틴 목록을 함께 반환합니다.
     * @return 
     */
    @GetMapping("/collections")
    public ResponseEntity < AllCollectionsResponseDto > getAllRoutineCollections() {
        AllCollectionsResponseDto responseDto = routineService.getAllRoutineCollections();
        return ResponseEntity.ok(responseDto);
    }
}