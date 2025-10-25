package com.rus.rus.application;

import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.req.ChatMessageRequestDto;
import com.rus.rus.controller.dto.res.*;
import com.rus.rus.domain.ChatMessage;
import com.rus.rus.domain.ChatSession;
import com.rus.rus.domain.UserProfile;
import com.rus.rus.infra.repository.ChatMessageRepository;
import com.rus.rus.infra.repository.ChatSessionRepository;
import com.rus.rus.infra.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.rus.rus.controller.dto.ChatMessageDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final UserProfileRepository userProfileRepository;
    private final OpenAIService openAIService; // OpenAI API 호출 서비스

    // 1. 챗봇 메시지 전송
    @Transactional
    public ChatMessageResponseDto sendMessage(String uid, ChatMessageRequestDto request) {
        // 사용자 확인
        UserProfile userProfile = userProfileRepository.findById(uid)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 세션 확인 또는 생성
        ChatSession session;
        if (request.getSessionId() != null) {
            session = chatSessionRepository.findBySessionIdAndUserProfile_Uid(request.getSessionId(), uid)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."));
        } else {
            session = createNewSession(userProfile);
        }

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .userProfile(userProfile)
                .message(request.getMessage())
                .messageType("USER")
                .timestamp(LocalDateTime.now())
                .tokenCount(estimateTokenCount(request.getMessage()))
                .build();
        chatMessageRepository.save(userMessage);

        // OpenAI API 호출
        String aiResponse = openAIService.generateResponse(request.getMessage(), request.getContext());
        int responseTokenCount = estimateTokenCount(aiResponse);

        // AI 응답 저장
        ChatMessage botMessage = ChatMessage.builder()
                .chatSession(session)
                .userProfile(userProfile)
                .message(aiResponse)
                .messageType("BOT")
                .timestamp(LocalDateTime.now())
                .tokenCount(responseTokenCount)
                .build();
        chatMessageRepository.save(botMessage);

        // 세션 업데이트 시간 갱신
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);

        return ChatMessageResponseDto.builder()
                .response(aiResponse)
                .sessionId(session.getSessionId())
                .timestamp(botMessage.getTimestamp())
                .messageType("BOT")
                .tokenCount(responseTokenCount)
                .build();
    }

    // 2. 스트리밍 메시지
    @Transactional
    public SseEmitter streamMessage(String uid, ChatMessageRequestDto request) {
        // 사용자 확인
        UserProfile userProfile = userProfileRepository.findById(uid)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 세션 확인 또는 생성
        ChatSession session;
        if (request.getSessionId() != null) {
            session = chatSessionRepository.findBySessionIdAndUserProfile_Uid(request.getSessionId(), uid)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."));
        } else {
            session = createNewSession(userProfile);
        }

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .userProfile(userProfile)
                .message(request.getMessage())
                .messageType("USER")
                .timestamp(LocalDateTime.now())
                .tokenCount(estimateTokenCount(request.getMessage()))
                .build();
        chatMessageRepository.save(userMessage);

        // SSE Emitter 생성 및 스트리밍
        SseEmitter emitter = new SseEmitter(60000L); // 60초 타임아웃

        final ChatSession finalSession = session;
        openAIService.streamResponse(request.getMessage(), request.getContext(), emitter,
                (fullResponse, tokenCount) -> {
                    // 스트리밍 완료 후 DB에 저장
                    ChatMessage botMessage = ChatMessage.builder()
                            .chatSession(finalSession)
                            .userProfile(userProfile)
                            .message(fullResponse)
                            .messageType("BOT")
                            .timestamp(LocalDateTime.now())
                            .tokenCount(tokenCount)
                            .build();
                    chatMessageRepository.save(botMessage);

                    // 세션 업데이트
                    finalSession.setUpdatedAt(LocalDateTime.now());
                    chatSessionRepository.save(finalSession);
                });

        return emitter;
    }

    // 3. 채팅 히스토리 조회
    @Transactional(readOnly = true)
    public ChatHistoryResponseDto getChatHistory(String uid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<ChatMessage> messagePage = chatMessageRepository.findByUserProfile_Uid(uid, pageable);

        List<ChatMessageDto> messages = messagePage.getContent().stream()
                .map(msg -> ChatMessageDto.builder()
                        .id(msg.getId())
                        .message(msg.getMessage())
                        .messageType(msg.getMessageType())
                        .timestamp(msg.getTimestamp())
                        .tokenCount(msg.getTokenCount())
                        .build())
                .collect(Collectors.toList());

        return ChatHistoryResponseDto.builder()
                .messages(messages)
                .currentPage(page)
                .totalPages(messagePage.getTotalPages())
                .totalMessages(messagePage.getTotalElements())
                .build();
    }

    // 4. 새 세션 생성
    @Transactional
    public ChatSessionResponseDto createSession(String uid) {
        UserProfile userProfile = userProfileRepository.findById(uid)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        ChatSession session = createNewSession(userProfile);

        return ChatSessionResponseDto.builder()
                .sessionId(session.getSessionId())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .active(session.getActive())
                .title(session.getTitle())
                .build();
    }

    // 5. 사용자 세션 목록 조회
    @Transactional(readOnly = true)
    public List<ChatSessionResponseDto> getUserSessions(String uid) {
        List<ChatSession> sessions = chatSessionRepository.findByUserProfile_UidOrderByUpdatedAtDesc(uid);

        return sessions.stream()
                .map(session -> ChatSessionResponseDto.builder()
                        .sessionId(session.getSessionId())
                        .createdAt(session.getCreatedAt())
                        .updatedAt(session.getUpdatedAt())
                        .active(session.getActive())
                        .title(session.getTitle())
                        .build())
                .collect(Collectors.toList());
    }

    // 6. 세션 삭제 (비활성화)
    @Transactional
    public void deleteSession(String uid, String sessionId) {
        ChatSession session = chatSessionRepository.findBySessionIdAndUserProfile_Uid(sessionId, uid)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."));

        session.setActive(false);
        chatSessionRepository.save(session);
    }

    // Private 헬퍼 메서드
    private ChatSession createNewSession(UserProfile userProfile) {
        ChatSession session = ChatSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .userProfile(userProfile)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .active(true)
                .title("새 대화")
                .build();

        return chatSessionRepository.save(session);
    }

    private int estimateTokenCount(String text) {
        // 간단한 토큰 추정 (실제로는 더 정확한 방법 사용)
        return (int) Math.ceil(text.length() / 4.0);
    }
}