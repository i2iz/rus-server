package com.rus.rus.controller;

import com.rus.rus.application.RoutineAdditionService;
import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.req.AddRecommendedRoutineRequestDto;
import com.rus.rus.controller.dto.req.BulkAddRoutineRequestDto;
import com.rus.rus.controller.dto.req.ManualAddRoutineRequestDto;
import com.rus.rus.controller.dto.res.AddRoutineResponseDto;
import com.rus.rus.controller.dto.res.BulkAddRoutineResponseDto;
import com.rus.rus.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 챗봇 추천 루틴 추가 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chatbot/routines")
public class RoutineAdditionController {

    private final RoutineAdditionService routineAdditionService;
    private final JwtUtil jwtUtil;

    // ==================== AI 메시지에서 루틴 추가 ====================
    /**
     * AI 챗봇 메시지에서 추천된 루틴을 사용자에게 추가
     *
     * @param uid 사용자 ID
     * @param request 추가할 메시지 ID와 선택된 루틴 인덱스
     * @param authHeader JWT 인증 헤더
     * @return 추가 결과 목록
     */
    @PostMapping("/add-from-message/{uid}")
    public ResponseEntity<BulkAddRoutineResponseDto> addRoutinesFromMessage(
            @PathVariable String uid,
            @RequestBody AddRecommendedRoutineRequestDto request,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 루틴을 추가할 권한이 없습니다.");
        }

        log.info("POST /chatbot/routines/add-from-message/{} - messageId: {}",
                uid, request.getMessageId());

        List<AddRoutineResponseDto> results = routineAdditionService.addRecommendedRoutines(uid, request);

        long successCount = results.stream().filter(AddRoutineResponseDto::isSuccess).count();
        long failureCount = results.size() - successCount;

        BulkAddRoutineResponseDto response = BulkAddRoutineResponseDto.builder()
                .totalRequested(results.size())
                .successCount((int) successCount)
                .failureCount((int) failureCount)
                .results(results)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==================== 수동 루틴 추가 ====================
    /**
     * 수동으로 루틴 추가
     *
     * @param uid 사용자 ID
     * @param request 추가할 루틴 정보
     * @param authHeader JWT 인증 헤더
     * @return 추가 결과
     */
    @PostMapping("/add-manual/{uid}")
    public ResponseEntity<AddRoutineResponseDto> addManualRoutine(
            @PathVariable String uid,
            @RequestBody ManualAddRoutineRequestDto request,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 루틴을 추가할 권한이 없습니다.");
        }

        log.info("POST /chatbot/routines/add-manual/{} - routine: {}",
                uid, request.getName());

        AddRoutineResponseDto result = routineAdditionService.addManualRoutine(uid, request);

        return ResponseEntity.ok(result);
    }

    // ==================== 일괄 루틴 추가 ====================
    /**
     * 여러 루틴을 일괄 추가
     *
     * @param uid 사용자 ID
     * @param request 추가할 루틴 목록
     * @param authHeader JWT 인증 헤더
     * @return 일괄 추가 결과
     */
    @PostMapping("/add-bulk/{uid}")
    public ResponseEntity<BulkAddRoutineResponseDto> addBulkRoutines(
            @PathVariable String uid,
            @RequestBody BulkAddRoutineRequestDto request,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 루틴을 추가할 권한이 없습니다.");
        }

        log.info("POST /chatbot/routines/add-bulk/{} - count: {}",
                uid, request.getRoutines().size());

        List<AddRoutineResponseDto> results = request.getRoutines().stream()
                .map(routineRequest -> routineAdditionService.addManualRoutine(uid, routineRequest))
                .toList();

        long successCount = results.stream().filter(AddRoutineResponseDto::isSuccess).count();
        long failureCount = results.size() - successCount;

        BulkAddRoutineResponseDto response = BulkAddRoutineResponseDto.builder()
                .totalRequested(results.size())
                .successCount((int) successCount)
                .failureCount((int) failureCount)
                .results(results)
                .build();

        return ResponseEntity.ok(response);
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
}