package com.rus.rus.controller;

import com.rus.rus.application.ChatbotService;
import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.req.ChatMessageRequestDto;
import com.rus.rus.controller.dto.res.ChatMessageResponseDto;
import com.rus.rus.controller.dto.res.ChatHistoryResponseDto;
import com.rus.rus.controller.dto.res.ChatSessionResponseDto;
import com.rus.rus.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final JwtUtil jwtUtil;

    // 1. 챗봇 메시지 전송
    @PostMapping("/message/{uid}")
    public ResponseEntity<ChatMessageResponseDto> sendMessage(
            @PathVariable String uid,
            @RequestBody ChatMessageRequestDto request,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 챗봇을 사용할 권한이 없습니다.");
        }

        ChatMessageResponseDto response = chatbotService.sendMessage(uid, request);
        return ResponseEntity.ok(response);
    }

    // 2. 스트리밍 챗봇 응답
    @PostMapping(value = "/stream/{uid}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @PathVariable String uid,
            @RequestBody ChatMessageRequestDto request,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 챗봇을 사용할 권한이 없습니다.");
        }

        return chatbotService.streamMessage(uid, request);
    }

    // 3. 채팅 히스토리 조회
    @GetMapping("/history/{uid}")
    public ResponseEntity<ChatHistoryResponseDto> getChatHistory(
            @PathVariable String uid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 채팅 기록을 조회할 권한이 없습니다.");
        }

        if (size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "페이지 크기는 100을 초과할 수 없습니다.");
        }

        ChatHistoryResponseDto response = chatbotService.getChatHistory(uid, page, size);
        return ResponseEntity.ok(response);
    }

    // 4. 새 채팅 세션 시작
    @PostMapping("/session/{uid}")
    public ResponseEntity<ChatSessionResponseDto> createSession(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 세션을 생성할 권한이 없습니다.");
        }

        ChatSessionResponseDto response = chatbotService.createSession(uid);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 5. 사용자 세션 목록 조회
    @GetMapping("/sessions/{uid}")
    public ResponseEntity<List<ChatSessionResponseDto>> getSessions(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 세션을 조회할 권한이 없습니다.");
        }

        List<ChatSessionResponseDto> sessions = chatbotService.getUserSessions(uid);
        return ResponseEntity.ok(sessions);
    }

    // 6. 세션 삭제
    @DeleteMapping("/session/{uid}/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String uid,
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String authHeader) {

        String tokenUid = extractUidFromToken(authHeader);

        if (!tokenUid.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "해당 사용자의 세션을 삭제할 권한이 없습니다.");
        }

        chatbotService.deleteSession(uid, sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * JWT 토큰에서 uid 추출
     */

    private String extractUidFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 헤더입니다.");
        }

        String token = authHeader.substring(7);

        try {
            return jwtUtil.getUidFromToken(token);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT 토큰입니다.");
        }
    }

    // ChatbotController.java 맨 아래에 추가
    @PostMapping("/test/token")
    public ResponseEntity<Map<String, String>> generateToken(@RequestParam String uid) {
        try {
            String token = jwtUtil.generateToken(uid);

            Map<String, String> response = new HashMap<>();
            response.put("uid", uid);
            response.put("token", token);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 생성 실패");
        }
    }
}


