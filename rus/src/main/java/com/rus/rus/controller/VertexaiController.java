package com.rus.rus.controller;

import com.rus.rus.application.TtsService;
import com.rus.rus.application.VertexaiService;
import com.rus.rus.controller.dto.ChatMessageDto;
import com.rus.rus.controller.dto.req.ChatRequestDto;
import com.rus.rus.controller.dto.res.ChatResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/vertexai")
@ConditionalOnProperty(name = "vertex.enabled", havingValue = "true")
public class VertexaiController {

  // (다음 단계에서 ChatbotService를 구현하고 주입해야 합니다)
  private final VertexaiService vertexaiService;
  private final TtsService ttsService;

  /**
   * 챗봇 메시지 전송 및 응답 API
   *
   * @param requestDto 클라이언트가 보낸 전체 대화 기록
   * @param uid      JWT 토큰에서 추출된 사용자 식별자
   * @return AI의 최종 텍스트 응답
   * @throws IOException Vertex AI 통신 오류
   */
  @PostMapping("/message")
  public ResponseEntity<ChatResponseDto> handleChatMessage(
          @RequestBody ChatRequestDto requestDto,
          @AuthenticationPrincipal UserDetails userDetails,
          @RequestParam(value = "speak", required = false, defaultValue = "false") boolean speak
  ) throws Exception {

    // ✅ uid 변수 선언
    UUID uid = UUID.fromString(userDetails.getUsername());

    // ✅ ttsService는 아래 주입되어야 함
    String aiResponseText = vertexaiService.getChatResponse(uid.toString(), requestDto.getMessages());

    String audioB64 = null;
    if (speak) {
      byte[] mp3 = ttsService.synthesize(aiResponseText);
      audioB64 = java.util.Base64.getEncoder().encodeToString(mp3);
    }

    ChatResponseDto response = new ChatResponseDto(
            new ChatMessageDto("MODEL", aiResponseText),
            audioB64
    );
    return ResponseEntity.ok(response);
  }

}