package com.rus.rus.controller;

import java.util.Arrays;
import java.util.List;

import com.rus.rus.controller.dto.CollectionDetailDto;
import com.rus.rus.controller.dto.req.*;
import com.rus.rus.controller.dto.res.*;
import com.rus.rus.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rus.rus.application.ChallengeService;
import com.rus.rus.application.RoutineService;
import com.rus.rus.common.ApiException;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/routine")
public class RoutineController {

    private static final List<String> ALLOWED_CATEGORIES = Arrays.asList("수면", "운동", "영양소", "햇빛", "사회적유대감");

    private final RoutineService routineService;
    private final ChallengeService challengeService;
    private final JwtUtil jwtUtil;

    @GetMapping("/recommend")
    public ResponseEntity<RecommendResponseDto> getRecommendedRoutines(
            @RequestParam("category") List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "하나 이상의 카테고리를 지정해야 합니다.");
        }

        for (String categoryName : categories) {
            if (!ALLOWED_CATEGORIES.contains(categoryName)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "유효하지 않은 카테고리 이름입니다: " + categoryName);
            }
        }

        RecommendResponseDto responseDto = routineService.getRecommendedRoutines(categories);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping
    public ResponseEntity<AllRoutinesResponseDto> getAllRoutines() {
        AllRoutinesResponseDto responseDto = routineService.getAllRoutines();
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/challenge")
    public ResponseEntity<ChallengeResponseDto> getChallengeInfo() {
        ChallengeResponseDto responseDto = challengeService.getChallengeInfo();
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/collections")
    public ResponseEntity<AllCollectionsResponseDto> getAllRoutineCollections() {
        AllCollectionsResponseDto responseDto = routineService.getAllRoutineCollections();
        return ResponseEntity.ok(responseDto);
    }

    // ==================== 4-1. 루틴 추가 ====================
    @PostMapping("/add/{uid}")
    public ResponseEntity<Void> addRoutine(
            @PathVariable String uid,
            @RequestBody RoutineAddRequestDto request,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 루틴을 추가할 권한이 없습니다.");
        }

        routineService.addRoutinesToUser(uid, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ==================== 4-2. 루틴 추가 - 직접 작성 ====================
    @PostMapping("/add/custom/{uid}")
    public ResponseEntity<Void> addCustomRoutine(
            @PathVariable String uid,
            @RequestBody RoutineAddCustomRequestDto request,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 루틴을 추가할 권한이 없습니다.");
        }

        routineService.addCustomRoutineToUser(uid, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ==================== 4-3. 요청 날짜 기준 사용자의 루틴 정보 반환 ====================
    @GetMapping("/personal/{uid}")
    public ResponseEntity<PersonalRoutineResponseDto> getPersonalRoutines(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 루틴을 조회할 권한이 없습니다.");
        }

        PersonalRoutineResponseDto response = routineService.getPersonalRoutines(uid);
        return ResponseEntity.ok(response);
    }

    // ==================== 4-4. 특정 루틴 정보 반환 ====================
    @GetMapping("/users/{id}")
    public ResponseEntity<SingleRoutineResponseDto> getUserRoutine(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        SingleRoutineResponseDto response = routineService.getRoutineById(id, tokenUid);
        return ResponseEntity.ok(response);
    }

    // ==================== 4-5. 사용자 루틴 - 루틴 수정 ====================
    @PatchMapping("/personal/{id}")
    public ResponseEntity<Void> updateRoutine(
            @PathVariable Integer id,
            @RequestBody RoutineUpdateRequestDto request,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        routineService.updateRoutine(id, request, tokenUid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ==================== 4-6. 사용자 루틴 - 루틴 삭제 ====================
    @DeleteMapping("/personal/{id}")
    public ResponseEntity<Void> deleteRoutine(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        routineService.deleteRoutine(id, tokenUid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ==================== 4-7. 사용자 루틴 - 달성 체크 ====================
    @PostMapping("/personal/attainment/{uid}")
    public ResponseEntity<Void> checkAttainment(
            @PathVariable String uid,
            @RequestBody AttainmentRequestDto request,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 루틴을 체크할 권한이 없습니다.");
        }

        routineService.checkRoutineAttainment(uid, request.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ==================== 4-8. 사용자 루틴 - 체크 해제 ====================
    @DeleteMapping("/personal/attainment/{uid}")
    public ResponseEntity<Void> uncheckAttainment(
            @PathVariable String uid,
            @RequestParam Integer id,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 루틴을 체크 해제할 권한이 없습니다.");
        }

        routineService.uncheckRoutineAttainment(uid, id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ==================== 4-9. 요청 날짜 기준 사용자의 추천 루틴 정보 반환 ====================
    @GetMapping("/recommend/{uid}")
    public ResponseEntity<RecommendRoutineResponseDto> getRecommendRoutines(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 추천 루틴을 조회할 권한이 없습니다.");
        }

        RecommendRoutineResponseDto response = routineService.getRecommendRoutines(uid);
        return ResponseEntity.ok(response);
    }

    // ==================== 4-10. Sera의 추천 루틴 - 달성 체크 ====================
    @PostMapping("/recommend/attainment/{uid}")
    public ResponseEntity<Void> checkRecommendAttainment(
            @PathVariable String uid,
            @RequestBody AttainmentRequestDto request,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 추천 루틴을 체크할 권한이 없습니다.");
        }

        routineService.checkSeraRoutineAttainment(uid, request.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ==================== 4-11. 챌린지 미션 - 발생 여부 조회 ====================
    @GetMapping("/challenge/istarget/{uid}")
    public ResponseEntity<ChallengeStatusResponseDto> getChallengeStatus(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 챌린지를 조회할 권한이 없습니다.");
        }

        ChallengeStatusResponseDto response = routineService.getChallengeStatus(uid);
        return ResponseEntity.ok(response);
    }

    // ==================== 4-12. 챌린지 미션 - 도전 수락 ====================
    @PatchMapping("/challenge/acceptance/{uid}")
    public ResponseEntity<Void> acceptChallenge(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 챌린지를 수락할 권한이 없습니다.");
        }

        routineService.acceptChallenge(uid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ==================== 4-13. 챌린지 미션 - 다음에 ====================
    @DeleteMapping("/challenge/hold/{uid}")
    public ResponseEntity<Void> postponeChallenge(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 챌린지를 보류할 권한이 없습니다.");
        }

        routineService.postponeChallenge(uid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ==================== 4-14. 챌린지 미션 - 달성 체크 ====================
    @PatchMapping("/challenge/attainment/{uid}")
    public ResponseEntity<Void> completeChallengeAttainment(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 챌린지를 완료할 권한이 없습니다.");
        }

        routineService.completeChallengeAttainment(uid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * JWT 토큰에서 uid 추출
     */
    private String extractUidFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 헤더입니다.");
        }

        String token = authHeader.substring(7); // "Bearer " 제거

        try {
            return jwtUtil.getUidFromToken(token);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT 토큰입니다.");
        }
    }

    /**
     * 특정 루틴 컬렉션 ID에 해당하는 루틴 모음 정보를 반환합니다.
     * 
     * @param collectionId 루틴 컬렉션 ID
     * @return 특정 컬렉션의 상세 정보
     */
    @GetMapping("/collections/{collectionId}")
    public ResponseEntity<CollectionDetailDto> getRoutineCollection(
            @PathVariable Integer collectionId) {
        CollectionDetailDto responseDto = routineService.getRoutineCollectionById(collectionId);
        return ResponseEntity.ok(responseDto);
    }
}