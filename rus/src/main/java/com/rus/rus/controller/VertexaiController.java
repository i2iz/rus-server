package com.rus.rus.controller;

import com.rus.rus.application.VertexaiService;
import com.rus.rus.controller.dto.ChatMessageDto;
import com.rus.rus.controller.dto.req.ChatRequestDto;
import com.rus.rus.controller.dto.res.ChatResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/vertexai")
public class VertexaiController {

  // (다음 단계에서 ChatbotService를 구현하고 주입해야 합니다)
  private final VertexaiService vertexaiService;

  /**
   * 챗봇 메시지 전송 및 응답 API
   * 
   * @param requestDto 클라이언트가 보낸 전체 대화 기록
   * @param uid        JWT 토큰에서 추출된 사용자 식별자
   * @return AI의 최종 텍스트 응답
   * @throws IOException Vertex AI 통신 오류
   */
  @PostMapping("/message")
  public ResponseEntity<ChatResponseDto> handleChatMessage(
      @RequestBody ChatRequestDto requestDto,
      @AuthenticationPrincipal UserDetails userDetails) throws IOException {
    UUID uid = UUID.fromString(userDetails.getUsername());

    String aiResponseText = vertexaiService.getChatResponse(uid, requestDto.getMessages());

    ChatResponseDto response = new ChatResponseDto(
        new ChatMessageDto("MODEL", aiResponseText));

    return ResponseEntity.ok(response);
  }
}